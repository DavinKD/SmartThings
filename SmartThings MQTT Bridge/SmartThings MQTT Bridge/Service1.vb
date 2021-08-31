Imports System.Threading
Imports MQTTnet
Imports MQTTnet.Server
Imports System.IO
Imports System.Net
Imports System.Text
Imports Newtonsoft.Json.Linq
Imports Newtonsoft.Json
Imports System.Collections.Concurrent
Imports System.Data.SqlClient
Imports MQTTnet.Client.Options
Imports MQTTnet.Client.Disconnecting

Public Class SmartThingsMQTTService1

    Dim WithEvents myMQTT As New MQTTnet.MqttFactory
    Dim WithEvents myServer As IMqttServer
    Dim token As String = ""
    Dim oDevices As New List(Of clsDevice)
    Dim gLogDir As String = ""
    Dim bLogAllMessages As Boolean = False
    Dim TimerConfig As New Timers.Timer
    Dim TimerQueue As New Timers.Timer
    Dim bStopping As Boolean = False
    Dim iLogLevel As Integer = 0 '0 = errors, 1 = warning, 2 = info, 3=Debug
    Dim dtLastTimeVersionChecked As DateTime = CDate("1/1/1900")
    Dim dtLastTimeMessageLogCleared As DateTime = CDate("1/1/1900")
    Dim bKissingGame As Boolean = False
    Dim gConnectionString As String = "server=MINI1\SQLEXPRESS;Database=SmartHouse;Trusted_Connection=True;"
    'Dim tryCount(5) As Double
    'Dim hitCount(5) As Double
    Private Class clsEvent
        Public iStartSecond As Integer
        Public iLength As Integer
        Public bDone As Boolean = False
    End Class

    Private Class tMessage
        Public sDevice As String
        Public sData As String
        Public gMessageGuid As Guid
    End Class

    Private Class clsAttribute
        Public da_id As Guid
        Public sKey As String
    End Class

    Private Class clsDeviceAttribute
        Public md_id As Guid
        Public dta_id As Guid
        Public da_attributeName As String
        Public da_attributeJSON1 As String
        Public da_attributeJSON2 As String
        Public da_attributeJSON3 As String
        Public da_attributeJSON4 As String
    End Class

    Private Class clsDevice
        Public sDeviceId As String
        Public sTopic As String
        Public bFoundInConfig As Boolean
    End Class

    Dim tQueue As ConcurrentQueue(Of tMessage) = New ConcurrentQueue(Of tMessage)()
    Dim processQueueThread1 As New Thread(Sub() processQueue(1))
    Dim processQueueThread2 As New Thread(Sub() processQueue(2))
    Dim processQueueThread3 As New Thread(Sub() processQueue(3))
    Dim processQueueThread4 As New Thread(Sub() processQueue(4))
    Dim processQueueThread5 As New Thread(Sub() processQueue(5))
    Dim processTimedEventsThread As New Thread(Sub() processTimedEvents())
    'Dim processPingsThread As New Thread(Sub() processPings())
    Dim kissingGameThread As New Thread(Sub() kissingGame())

    Protected Overrides Sub OnStart(ByVal args() As String)
        Try
            WriteToErrorLog("OnStart(): IN", 3)
            Dim objServerOptions As New MqttServerOptions()
            Dim gAppDir = My.Application.Info.DirectoryPath
            Dim i As Integer
            'gLogDir = gAppDir
            myServer = myMQTT.CreateMqttServer()
            objServerOptions.EnablePersistentSessions = True
            myServer.UseApplicationMessageReceivedHandler(AddressOf OnApplicationMessageReceived)
            myServer.StartAsync(objServerOptions)
            readConfig()
            readDeviceList()

            'For i = 1 To 5
            '    tryCount(i) = 0
            '    hitCount(i) = 0
            'Next
            AddHandler TimerConfig.Elapsed, AddressOf TimerX_Tick
            With TimerConfig
                .Interval = 60000
                .Enabled = True
            End With

            With kissingGameThread
                .IsBackground = True ' not necessary...
                .Start()
            End With



            With processQueueThread1
                .IsBackground = True ' not necessary...
                .Start()
            End With
            With processQueueThread2
                .IsBackground = True ' not necessary...
                .Start()
            End With
            With processQueueThread3
                .IsBackground = True ' not necessary...
                .Start()
            End With
            With processQueueThread4
                .IsBackground = True ' not necessary...
                .Start()
            End With
            With processQueueThread5
                .IsBackground = True ' not necessary...
                .Start()
            End With

            With processTimedEventsThread
                .IsBackground = True
                .Start()
            End With

            'With processPingsThread
            '.IsBackground = True
            '.Start()
            'End With

            WriteToErrorLog("OnStart(): OUT", 3)
        Catch ex As Exception
            WriteToErrorLog("OnStart()" & Err.Description)
        End Try

    End Sub


    Private Sub TimerX_Tick(ByVal sender As System.Object, ByVal e As System.EventArgs)
        readConfig()
        readDeviceList()
    End Sub

    'Sub checkQueue()
    '    While Not bStopping
    '        Try
    '            If Not processQueueThread.IsAlive Then
    '                WriteToErrorLog("checkQueue(): ProcessQueueThread was crashed!")
    '                With processQueueThread
    '                    .IsBackground = True ' not necessary...
    '                    .Start()
    '                End With
    '            End If
    '        Catch ex As Exception
    '            WriteToErrorLog("checkQueue(): " & Err.Description)
    '        End Try
    '    End While
    'End Sub
    Async Sub processQueue(inId As Integer)
        While Not bStopping
            Try
                'Try
                '    tryCount(inId) += 1
                'Catch ex As Exception
                '    tryCount(inId) = 0
                '    hitCount(inId) = 0
                'End Try
                Dim lMessage As New tMessage
                'lMessage = tQueue.Take(1)
                If tQueue.TryDequeue(lMessage) Then
                    'WriteToErrorLog("Processing " & lMessage.sDevice & "-" & lMessage.sData)
                    'Try
                    '    hitCount(inId) += 1
                    'Catch ex As Exception
                    '    tryCount(inId) = 0
                    '    hitCount(inId) = 0
                    'End Try
                    Await sendData(lMessage.sDevice, lMessage.sData, lMessage.gMessageGuid)
                Else
                End If
            Catch ex As Exception
                WriteToErrorLog("processQueue(): " & Err.Description)
            End Try
            Thread.Sleep(20)
            'WriteToErrorLog("Still Going")
        End While
        'WriteToErrorLog("Thread " & inId & " was " & hitCount(inId) & " out of " & tryCount(inId))
    End Sub

    Sub processTimedEvents()
        Thread.Sleep(30000)
        While Not bStopping
            Try
                Dim gConnection As New SqlConnection
                Dim gCommand As New SqlCommand
                gConnection.ConnectionString = gConnectionString
                gConnection.Open()
                If DateAdd(DateInterval.Hour, 24, dtLastTimeVersionChecked) < Now Then
                    dtLastTimeVersionChecked = Now
                    gCommand.Connection = gConnection
                    gCommand.CommandText = "select * from mqttDevice"
                    Dim gReader As SqlDataReader = gCommand.ExecuteReader
                    While gReader.Read
                        updateVersion(gReader("md_topic"))
                    End While
                    If Not gReader.IsClosed Then
                        gReader.Close()
                    End If
                End If
                If DateAdd(DateInterval.Hour, 24, dtLastTimeMessageLogCleared) < Now Then
                    dtLastTimeMessageLogCleared = Now
                    gCommand = New SqlCommand
                    gCommand.Connection = gConnection
                    gCommand.CommandText = "delete from messageLog where ml_timestamp < '" & DateAdd(DateInterval.Hour, -72, Now) & "'"
                    gCommand.ExecuteNonQuery()

                    gCommand = New SqlCommand
                    gCommand.Connection = gConnection
                    gCommand.CommandText = "delete from deviceAttributeHistory where dah_timestamp < '" & DateAdd(DateInterval.Hour, -168, Now) & "'"
                    gCommand.ExecuteNonQuery()
                End If
                gCommand = New SqlCommand
                gCommand.Connection = gConnection
                gCommand.CommandText = "Select * from mqttDevice where md_upgradeRequested = 'true'"
                Dim gReader2 As SqlDataReader = gCommand.ExecuteReader
                While gReader2.Read
                    upgradeDevice(gReader2("md_topic"))
                End While
                If Not gReader2.IsClosed Then
                    gReader2.Close()
                End If
                gCommand = New SqlCommand
                gCommand.Connection = gConnection
                gCommand.CommandText = "Select mqttDevice.md_id, mqttDevice.md_description, deviceTypeAttribute.dt_id, deviceTypeAttribute.dta_id, dta_attributeName, dta_attributeJSON1, dta_attributeJSON2, dta_attributeJSON3, dta_attributeJSON4 from mqttDevice inner join deviceType on mqttdevice.dt_id = deviceType.dt_id inner join deviceTypeAttribute on deviceType.dt_id = deviceTypeAttribute.dt_id left outer join deviceAttribute on mqttdevice.md_id = deviceAttribute.md_id and devicetypeattribute.dta_id = deviceAttribute.dta_id where deviceAttribute.dta_id is null"
                Dim gReader3 As SqlDataReader = gCommand.ExecuteReader
                Dim lDeviceAttributes As New List(Of clsDeviceAttribute)

                While gReader3.Read
                    Dim lDeviceAttribute As New clsDeviceAttribute
                    lDeviceAttribute.md_id = gReader3("md_id")
                    lDeviceAttribute.da_attributeName = gReader3("dta_attributeName")
                    lDeviceAttribute.da_attributeJSON1 = gReader3("dta_attributeJSON1")
                    lDeviceAttribute.da_attributeJSON2 = gReader3("dta_attributeJSON2")
                    lDeviceAttribute.da_attributeJSON3 = gReader3("dta_attributeJSON3")
                    lDeviceAttribute.da_attributeJSON4 = gReader3("dta_attributeJSON4")
                    lDeviceAttribute.dta_id = gReader3("dta_id")
                    lDeviceAttributes.Add(lDeviceAttribute)
                End While
                If Not gReader3.IsClosed Then
                    gReader3.Close()
                End If

                For Each lDeviceAttribute In lDeviceAttributes
                    gCommand = New SqlCommand
                    gCommand.Connection = gConnection
                    gCommand.CommandText = "insert into deviceAttribute values (newid(), @param1, @param2, @param3, @param4, @param5, @param6, @param7, @param8, null)"
                    gCommand.Parameters.Add("@param1", SqlDbType.UniqueIdentifier).Value = lDeviceAttribute.md_id
                    gCommand.Parameters.Add("@param2", SqlDbType.UniqueIdentifier).Value = lDeviceAttribute.dta_id
                    gCommand.Parameters.Add("@param3", SqlDbType.NVarChar).Value = lDeviceAttribute.da_attributeName
                    gCommand.Parameters.Add("@param4", SqlDbType.NVarChar).Value = lDeviceAttribute.da_attributeJSON1
                    gCommand.Parameters.Add("@param5", SqlDbType.NVarChar).Value = lDeviceAttribute.da_attributeJSON2
                    gCommand.Parameters.Add("@param6", SqlDbType.NVarChar).Value = lDeviceAttribute.da_attributeJSON3
                    gCommand.Parameters.Add("@param7", SqlDbType.NVarChar).Value = lDeviceAttribute.da_attributeJSON4
                    gCommand.Parameters.Add("@param8", SqlDbType.NVarChar).Value = ""
                    gCommand.ExecuteNonQuery()
                Next





                If gConnection.State = ConnectionState.Open Then
                    gConnection.Close()
                End If
                'Dim i As Integer
                'For i = 1 To 5
                '    WriteToErrorLog("Thread " & i & " was " & hitCount(i) & " out of " & tryCount(i) & " or " & Format(hitCount(i) / tryCount(i) * 100, "0.00") & "%")
                'Next
                Thread.Sleep(30000)
            Catch ex As Exception
                WriteToErrorLog("processTimedEvents - " & Err.Description)
            End Try
        End While
    End Sub

    Sub processPings()
        Thread.Sleep(300000)
        While Not bStopping
            Try
                Dim gConnection As New SqlConnection
                Dim gCommand As New SqlCommand
                gConnection.ConnectionString = gConnectionString
                gConnection.Open()
                gCommand = New SqlCommand
                gCommand.Connection = gConnection
                gCommand.CommandText = "Select * from mqttDevice where md_lastMessageReceived > '" & DateAdd(DateInterval.Minute, -10, Now()) & "'"
                Dim gReader2 As SqlDataReader = gCommand.ExecuteReader
                While gReader2.Read
                    pingDevice(gReader2("md_topic"))
                End While
                If Not gReader2.IsClosed Then
                    gReader2.Close()
                End If
            Catch ex As Exception
                WriteToErrorLog("processTimedEvents - " & Err.Description)
            End Try
        End While
    End Sub

    Private Sub pingDevice(inTopic As String)

        Dim myMQTT As New MQTTnet.MqttFactory
        Dim myClient As MQTTnet.Client.IMqttClient

        Dim sTopic As String = "cmnd/" & inTopic & "/Time"
        Dim lCount As Integer = 0
        Try
            myClient = myMQTT.CreateMqttClient()
            Dim options = New MqttClientOptionsBuilder().WithTcpServer("127.0.0.1").Build()
            Dim disoptions = New MqttClientDisconnectOptions
            Dim cToken As New CancellationToken
            myClient.ConnectAsync(options, cToken)
            While Not myClient.IsConnected
                lCount += 1
                If lCount > 30 Then
                    WriteToErrorLog("upgradeDevice(" & inTopic & ") - Could not connect")
                    Exit Sub
                End If
                Thread.Sleep(100)
            End While

            Dim message = New MqttApplicationMessageBuilder().WithTopic(sTopic).WithPayload("").Build()
            myClient.PublishAsync(message, cToken)
            myClient.DisconnectAsync(disoptions, cToken)
            updateUpgradeDeviceStatus(inTopic)
        Catch ex As Exception
            WriteToErrorLog("upgradeDevice(" & inTopic & ") - " & Err.Description)
        End Try
    End Sub


    Private Function IsValidJson(ByVal strInput As String) As Boolean
        Try
            strInput = strInput.Trim()

            If (strInput.StartsWith("{") AndAlso strInput.EndsWith("}")) OrElse (strInput.StartsWith("[") AndAlso strInput.EndsWith("]")) Then

                Try
                    Dim obj = JToken.Parse(strInput)
                    Return True
                Catch jex As JsonReaderException
                    'Console.WriteLine(jex.Message)
                    Return False
                Catch ex As Exception
                    'Console.WriteLine(ex.ToString())
                    Return False
                End Try
            Else
                Return False
            End If

        Catch ex As Exception
            WriteToErrorLog("IsValidJson(): " & Err.Description)
            Return False
        End Try
    End Function


    Protected Overrides Sub OnStop()
        Try
            bStopping = True
            myServer.StopAsync()
        Catch ex As Exception
            WriteToErrorLog("OnStop(): " & Err.Description)
        End Try
    End Sub

    Public Sub updateMessageStatus(inGuid As Guid)
        Dim gConnection As New SqlConnection
        Dim gCommand As New SqlCommand
        Try

            gConnection.ConnectionString = gConnectionString
            gConnection.Open()
            gCommand.Connection = gConnection
            gCommand.CommandText = "update messageLog set ml_completed = @param1 where ml_id = @param2"
            gCommand.Parameters.Add("@param1", SqlDbType.DateTime).Value = Now
            gCommand.Parameters.Add("@param2", SqlDbType.UniqueIdentifier).Value = inGuid
            gCommand.ExecuteNonQuery()
            If gConnection.State = ConnectionState.Open Then
                gConnection.Close()
            End If
        Catch ex As Exception
            WriteToErrorLog("updateMessageStatus - " & Err.Description)

        End Try

    End Sub

    Public Sub updateUpgradeDeviceStatus(inTopic As String)
        Dim gConnection As New SqlConnection
        Dim gCommand As New SqlCommand
        Try

            gConnection.ConnectionString = gConnectionString
            gConnection.Open()
            gCommand.Connection = gConnection
            gCommand.CommandText = "update mqttdevice set md_upgraderequested = @param1"
            gCommand.Parameters.Add("@param1", SqlDbType.Bit).Value = False
            gCommand.ExecuteNonQuery()
            If gConnection.State = ConnectionState.Open Then
                gConnection.Close()
            End If
        Catch ex As Exception
            WriteToErrorLog("updateUpgradeDeviceStatus - " & Err.Description)

        End Try

    End Sub

    Private Sub upgradeDevice(inTopic As String)

        Dim myMQTT As New MQTTnet.MqttFactory
        Dim myClient As MQTTnet.Client.IMqttClient

        Dim sTopic As String = ""
        Dim sPayload As String = ""
        Dim sTopic2 As String = ""
        Dim sPayload2 As String = ""
        Dim lCount As Integer = 0
        sTopic = "cmnd/" & inTopic & "/OtaUrl"
        sTopic2 = "cmnd/" & inTopic & "/Upgrade"
        sPayload = "http://ota.tasmota.com/tasmota/release/tasmota.bin"
        sPayload2 = "1"
        Try
            myClient = myMQTT.CreateMqttClient()
            Dim options = New MqttClientOptionsBuilder().WithTcpServer("127.0.0.1").Build()
            Dim disoptions = New MqttClientDisconnectOptions
            Dim cToken As New CancellationToken
            myClient.ConnectAsync(options, cToken)
            While Not myClient.IsConnected
                lCount += 1
                If lCount > 30 Then
                    WriteToErrorLog("upgradeDevice(" & inTopic & ") - Could not connect")
                    Exit Sub
                End If
                Thread.Sleep(100)
            End While

            Dim message = New MqttApplicationMessageBuilder().WithTopic(sTopic).WithPayload(sPayload).Build()
            myClient.PublishAsync(message, cToken)
            Dim message2 = New MqttApplicationMessageBuilder().WithTopic(sTopic2).WithPayload(sPayload2).Build()
            myClient.PublishAsync(message2, cToken)
            myClient.DisconnectAsync(disoptions, cToken)
            updateUpgradeDeviceStatus(inTopic)
        Catch ex As Exception
            WriteToErrorLog("upgradeDevice(" & inTopic & ") - " & Err.Description)
        End Try
    End Sub
    Private Sub updateVersion(inTopic As String)
        Dim myMQTT As New MQTTnet.MqttFactory
        Dim myClient As MQTTnet.Client.IMqttClient

        Dim sTopic As String = ""
        Dim sPayload As String = ""
        Dim lCount As Integer = 0
        sTopic = "cmnd/" & inTopic & "/status"
        sPayload = "2"
        Try
            WriteToErrorLog("updateVersion(" & inTopic & ") - IN", 3)
            myClient = myMQTT.CreateMqttClient()
            Dim options = New MqttClientOptionsBuilder().WithTcpServer("127.0.0.1").Build()
            Dim disoptions = New MqttClientDisconnectOptions
            Dim cToken As New CancellationToken
            myClient.ConnectAsync(options, cToken)
            While Not myClient.IsConnected
                lCount += 1
                If lCount > 30 Then
                    WriteToErrorLog("updateVersion(" & inTopic & ") - Could not connect")
                    Exit Sub
                End If
                Thread.Sleep(100)
            End While

            Dim message = New MqttApplicationMessageBuilder().WithTopic(sTopic).WithPayload(sPayload).Build()
            myClient.PublishAsync(message, cToken)
            myClient.DisconnectAsync(disoptions, cToken)
        Catch ex As Exception
            WriteToErrorLog("updateVersion(" & inTopic & ") - " & Err.Description)
        End Try
    End Sub



    Private Function logMessage(inDevice As String, inMessage As String, inTopic As String) As Guid
        Dim gConnection As New SqlConnection
        Dim gCommand As New SqlCommand
        Dim bFoundDevice As Boolean = False
        Dim gDevice As Guid
        Dim ret As Guid
        Dim oValue As String
        Try

            'EED8A810-2234-48D4-9FDE-39321B7F9FF7 - Tasmota Software Type
            gConnection.ConnectionString = gConnectionString
            gConnection.Open()
            gCommand.Connection = gConnection
            gCommand.CommandText = "Select * From mqttDevice where md_Topic = '" & inDevice & "'"
            Dim gReader As IDataReader = gCommand.ExecuteReader
            While gReader.Read
                bFoundDevice = True
                gDevice = gReader("md_id")
            End While
            If Not gReader.IsClosed Then
                gReader.Close()
            End If
            If Not bFoundDevice Then
                gCommand = New SqlCommand
                gCommand.Connection = gConnection
                gCommand.CommandText = "Insert into mqttDevice (md_id, md_topic, md_description, md_lastmessageReceived, md_SoftwareType) values (@param1, @param2, @param3, @param4, @param5)"
                gDevice = Guid.NewGuid
                gCommand.Parameters.Add("@param1", SqlDbType.UniqueIdentifier).Value = gDevice
                gCommand.Parameters.Add("@param2", SqlDbType.VarChar).Value = inDevice
                gCommand.Parameters.Add("@param3", SqlDbType.VarChar).Value = "Unknown Device"
                gCommand.Parameters.Add("@param4", SqlDbType.DateTime).Value = Now
                gCommand.Parameters.Add("@param5", SqlDbType.UniqueIdentifier).Value = Guid.Parse("EED8A810-2234-48D4-9FDE-39321B7F9FF7")
                gCommand.ExecuteNonQuery()
            Else
                gCommand = New SqlCommand
                gCommand.Connection = gConnection
                gCommand.CommandText = "Update mqttDevice set md_lastMessageReceived = @param1 where md_Topic = @param2"
                gCommand.Parameters.Add("@param1", SqlDbType.DateTime).Value = Now
                gCommand.Parameters.Add("@param2", SqlDbType.VarChar).Value = inDevice
                gCommand.ExecuteNonQuery()
            End If
            gCommand = New SqlCommand
            gCommand.Connection = gConnection
            gCommand.CommandText = "Insert into messageLog (ml_id, ml_md_id, ml_message, ml_timestamp, ml_topic) values (@param1, @param2, @param3, @param4, @param5)"
            ret = Guid.NewGuid
            gCommand.Parameters.Add("@param1", SqlDbType.UniqueIdentifier).Value = ret
            gCommand.Parameters.Add("@param2", SqlDbType.UniqueIdentifier).Value = gDevice
            gCommand.Parameters.Add("@param3", SqlDbType.NText).Value = inMessage
            gCommand.Parameters.Add("@param4", SqlDbType.DateTime).Value = Now
            gCommand.Parameters.Add("@param5", SqlDbType.NText).Value = inTopic
            gCommand.ExecuteNonQuery()
            Dim messageJSON As JObject
            Try
                messageJSON = JObject.Parse(inMessage)
            Catch ex As Exception
            End Try
            If messageJSON IsNot Nothing Then
                gCommand = New SqlCommand
                gCommand.Connection = gConnection
                gCommand.CommandText = "Select * from deviceAttribute where md_id = @param1"
                gCommand.Parameters.Add("param1", SqlDbType.UniqueIdentifier).Value = gDevice
                gReader = gCommand.ExecuteReader
                Dim lAttributes As New List(Of clsAttribute)

                While gReader.Read
                    If Not IsDBNull(gReader("da_attributeJSON1")) And Not gReader("da_attributeJSON1") = "" Then
                        Dim lAttribute As New clsAttribute
                        lAttribute.da_id = gReader("da_id")
                        lAttribute.sKey = gReader("da_attributeJSON1")
                        lAttributes.Add(lAttribute)
                    End If
                    If Not IsDBNull(gReader("da_attributeJSON2")) And Not gReader("da_attributeJSON2") = "" Then
                        Dim lAttribute As New clsAttribute
                        lAttribute.da_id = gReader("da_id")
                        lAttribute.sKey = gReader("da_attributeJSON2")
                        lAttributes.Add(lAttribute)
                    End If
                    If Not IsDBNull(gReader("da_attributeJSON3")) And Not gReader("da_attributeJSON3") = "" Then
                        Dim lAttribute As New clsAttribute
                        lAttribute.da_id = gReader("da_id")
                        lAttribute.sKey = gReader("da_attributeJSON3")
                        lAttributes.Add(lAttribute)
                    End If
                    If Not IsDBNull(gReader("da_attributeJSON4")) And Not gReader("da_attributeJSON4") = "" Then
                        Dim lAttribute As New clsAttribute
                        lAttribute.da_id = gReader("da_id")
                        lAttribute.sKey = gReader("da_attributeJSON4")
                        lAttributes.Add(lAttribute)
                    End If
                End While
                If Not gReader.IsClosed Then
                    gReader.Close()
                End If

                For Each lattribute In lAttributes
                    Try
                        Dim sValue As String = messageJSON.SelectToken(lattribute.sKey)
                        If Not IsNothing(sValue) Then
                            Dim gCommandUpdate As New SqlCommand
                            gCommandUpdate.Connection = gConnection
                            gCommandUpdate.CommandText = "Update deviceAttribute set da_attributeValue = @param1, da_lastTimeUpdated = @param2 where da_id = @param3"
                            gCommandUpdate.Parameters.Add("param1", SqlDbType.NVarChar).Value = sValue
                            gCommandUpdate.Parameters.Add("param2", SqlDbType.DateTime).Value = Now
                            gCommandUpdate.Parameters.Add("param3", SqlDbType.UniqueIdentifier).Value = lattribute.da_id
                            gCommandUpdate.ExecuteNonQuery()

                            Dim gCommandHistorySelect As New SqlCommand
                            gCommandHistorySelect.Connection = gConnection
                            gCommandHistorySelect.CommandText = "select top 1 * from deviceAttributeHistory where da_id = @param1 order by dah_timestamp desc"
                            gCommandHistorySelect.Parameters.Add("param1", SqlDbType.UniqueIdentifier).Value = lattribute.da_id
                            gReader = gCommandHistorySelect.ExecuteReader
                            Dim bInsert As Boolean = True
                            While gReader.Read
                                If sValue = gReader("dah_attributeValue") Then
                                    'The data already matches, ignore
                                    bInsert = False
                                End If
                            End While
                            If Not gReader.IsClosed Then
                                gReader.Close()
                            End If

                            If bInsert Then
                                Dim gCommandHistory As New SqlCommand
                                gCommandHistory.Connection = gConnection
                                gCommandHistory.CommandText = "Insert into deviceAttributeHistory (dah_id, da_id, dah_attributeValue, dah_timestamp) values (@param1, @param2, @param3, @param4)"
                                gCommandHistory.Parameters.Add("param3", SqlDbType.NVarChar).Value = sValue
                                gCommandHistory.Parameters.Add("param4", SqlDbType.DateTime).Value = Now
                                gCommandHistory.Parameters.Add("param2", SqlDbType.UniqueIdentifier).Value = lattribute.da_id
                                ret = Guid.NewGuid
                                gCommandHistory.Parameters.Add("@param1", SqlDbType.UniqueIdentifier).Value = ret

                                gCommandHistory.ExecuteNonQuery()
                            End If


                        End If
                    Catch ex As Exception
                        WriteToErrorLog("attribute - " & lattribute.sKey & " - " & Err.Description)
                    End Try
                Next

                Try
                    oValue = messageJSON("StatusFWR")("Version")
                Catch ex As Exception
                End Try
                If oValue IsNot Nothing Then
                    gCommand = New SqlCommand
                    gCommand.Connection = gConnection
                    gCommand.CommandText = "Update mqttDevice set md_softwareVersion = @param1 where md_Topic = @param2"
                    gCommand.Parameters.Add("@param1", SqlDbType.VarChar).Value = Replace(messageJSON("StatusFWR")("Version"), "(tasmota)", "")
                    gCommand.Parameters.Add("@param2", SqlDbType.VarChar).Value = inDevice
                    gCommand.ExecuteNonQuery()
                End If
                Try
                    oValue = messageJSON("Version")
                Catch ex As Exception
                End Try
                If oValue IsNot Nothing Then
                    gCommand = New SqlCommand
                    gCommand.Connection = gConnection
                    gCommand.CommandText = "Update mqttDevice set md_softwareVersion = @param1 where md_Topic = @param2"
                    gCommand.Parameters.Add("@param1", SqlDbType.VarChar).Value = Replace(messageJSON("Version"), "(tasmota)", "")
                    gCommand.Parameters.Add("@param2", SqlDbType.VarChar).Value = inDevice
                    gCommand.ExecuteNonQuery()
                End If
            End If
            If gConnection.State = ConnectionState.Open Then
                gConnection.Close()
            End If

            Return ret
        Catch ex As Exception
            WriteToErrorLog("logMessage - " & Err.Description)

            Return ret
        End Try

    End Function

    Private Sub sleepSeconds(inSec As Integer)
        Thread.Sleep(inSec * 1000)
    End Sub

    Private Sub kissingGame()
        Dim clsEvents As New List(Of clsEvent)
        Dim oEvent As New clsEvent

        oEvent.iStartSecond = 22
        oEvent.iLength = 34
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 120
        oEvent.iLength = 15
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 162
        oEvent.iLength = 15
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 225
        oEvent.iLength = 17
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 281
        oEvent.iLength = 208
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 943
        oEvent.iLength = 28
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 1105
        oEvent.iLength = 95
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 1485
        oEvent.iLength = 39
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 1626
        oEvent.iLength = 15
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 1750
        oEvent.iLength = 175
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 1985
        oEvent.iLength = 15
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 2021
        oEvent.iLength = 23
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 2082
        oEvent.iLength = 15
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 2156
        oEvent.iLength = 15
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 2410
        oEvent.iLength = 31
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 2607
        oEvent.iLength = 80
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 2817
        oEvent.iLength = 15
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 2867
        oEvent.iLength = 38
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 2938
        oEvent.iLength = 15
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 3084
        oEvent.iLength = 21
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 3108
        oEvent.iLength = 82
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 3205
        oEvent.iLength = 292
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 3698
        oEvent.iLength = 47
        clsEvents.Add(oEvent)
        oEvent = New clsEvent

        oEvent.iStartSecond = 3911
        oEvent.iLength = 34
        clsEvents.Add(oEvent)
        oEvent = New clsEvent



        While Not bStopping
            If bKissingGame Then
                bKissingGame = False

                Dim dStartTime As DateTime = Now
                'Reset events to not being done
                For Each iEvent In clsEvents
                    iEvent.bDone = False
                Next

                While Not bStopping
                    If bKissingGame Then
                        'Start over
                        Exit While
                    End If
                    For Each iEvent In clsEvents
                        If iEvent.bDone = False And iEvent.iStartSecond <= Fix(Now.Subtract(dStartTime).TotalSeconds) Then
                            turnOnLight(iEvent.iLength)
                            iEvent.bDone = True
                        End If
                    Next
                    If Fix(Now.Subtract(dStartTime).TotalSeconds) >= 4000 Then Exit While
                    Thread.Sleep(10)
                End While
            End If
        End While
    End Sub

    Private Sub turnOnLight(iLength As Integer)
        Dim webClient As New System.Net.WebClient
        Dim dStartTime As DateTime = Now()

        Dim result As String = webClient.DownloadString("http://192.168.0.194/cm?cmnd=PulseTime1%20" & 100 + iLength)
        result = webClient.DownloadString("http://192.168.0.194/cm?cmnd=Power%20On")
        'Dim result As String = webClient.DownloadString("http://192.168.0.187/cm?cmnd=PulseTime2%20" & 100 + iLength)
        'result = webClient.DownloadString("http://192.168.0.187/cm?cmnd=Power2%20On")
        Dim iTimeTook As Integer = Now.Subtract(dStartTime).TotalSeconds
        sleepSeconds(iLength - iTimeTook)
    End Sub

    Sub OnApplicationMessageReceived(ByVal eventArgs As MqttApplicationMessageReceivedEventArgs)
        Try
            Dim sTopic = eventArgs.ApplicationMessage.Topic
            Dim sPayload = UnicodeBytesToString(eventArgs.ApplicationMessage.Payload)
            Dim sValue = eventArgs.ApplicationMessage.ToString
            Dim sTopicDevice As String = ""
            Dim lMessage As New tMessage
            Dim gMessage As Guid

            If sTopic = "cmnd/3692/KissingGame" Then
                bKissingGame = True
            End If

            sTopicDevice = getTopicDeviceByTopic(sTopic)
            gMessage = logMessage(sTopicDevice, sPayload, sTopic)
            If sPayload = "Offline" Then
                WriteToErrorLog("Payload:  " & sTopicDevice & " - [" & sPayload & "]", 2)
            Else
                WriteToErrorLog("Payload:  " & sTopicDevice & " - [" & sPayload & "]", 3)
            End If

            Dim bQueued As Boolean = False

            For Each device In getDeviceIdByTopic(sTopic)
                bQueued = True
                lMessage = New tMessage
                lMessage.sDevice = device.sDeviceId
                lMessage.sData = sPayload
                lMessage.gMessageGuid = gMessage
                tQueue.Enqueue(lMessage)
            Next
            If Not bQueued Then
                'There's no SmartThings device in the devicelist.cfg for this device.  Mark it as done since there was nothing to do.
                updateMessageStatus(gMessage)
            End If

        Catch ex As Exception
            WriteToErrorLog("OnApplicationMessageReceived():  " & Err.Description)
        End Try
    End Sub

    Private Function UnicodeBytesToString(
    ByVal bytes() As Byte) As String
        Try
            Return System.Text.Encoding.ASCII.GetString(bytes)
        Catch ex As Exception
            'WriteToErrorLog("UnicodeBytesToString(): " & Err.Description, 3)
            Return ""
        End Try
    End Function


    Private Function strToBoolean(sStr As String) As Boolean
        strToBoolean = False
        Select Case sStr.ToLower
            Case "true"
                strToBoolean = True
            Case "y"
                strToBoolean = True
            Case "yes"
                strToBoolean = True
        End Select

    End Function

    Private Function strToInteger(sStr As String) As Integer
        If IsNumeric(sStr) Then
            Return CInt(sStr)
        Else
            Return 0
        End If
    End Function

    Private Sub readConfig()
        Try
            Dim dir As String
            dir = System.IO.Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location)
            Dim path1 As String = dir & "\SmartThingsMQTT.cfg"
            Dim fileIn As New StreamReader(path1)
            Dim lineInfo(2) As String
            Dim strData As String
            Dim sDevice As New clsDevice

            While Not (fileIn.EndOfStream)
                strData = fileIn.ReadLine
                If Trim(strData) <> "" Then
                    lineInfo = Split(strData, "|")
                    Select Case lineInfo(0).ToLower
                        Case "smartthingstoken"
                            token = lineInfo(1)
                        Case "loglevel"
                            iLogLevel = strToInteger(lineInfo(1))
                        Case "connectionstring"
                            gConnectionString = lineInfo(1)
                        Case "logdir"
                            gLogDir = lineInfo(1)
                    End Select
                End If
            End While
            fileIn.Close()
        Catch ex As Exception
            WriteToErrorLog("ReadConfig(): " & Err.Description)
        End Try
    End Sub
    Private Sub readDeviceList()
        Try
            Dim dir As String
            dir = System.IO.Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location)
            Dim path1 As String = dir & "\deviceList.cfg"

            Dim fileIn As New StreamReader(path1)
            Dim lineInfo(2) As String
            Dim strData As String
            Dim sDevice As New clsDevice
            Dim bExists As Boolean

            For Each cDevice In oDevices
                cDevice.bFoundInConfig = False
            Next

            While Not (fileIn.EndOfStream)
                sDevice = New clsDevice
                strData = fileIn.ReadLine
                If Trim(strData) <> "" Then
                    lineInfo = Split(strData, "=")
                    sDevice.sTopic = lineInfo(0)
                    sDevice.sDeviceId = lineInfo(1)
                    sDevice.bFoundInConfig = True
                    bExists = False
                    For Each cDevice In oDevices
                        If cDevice.sDeviceId = sDevice.sDeviceId And cDevice.sTopic = sDevice.sTopic Then
                            bExists = True
                            cDevice.bFoundInConfig = True
                        End If
                    Next
                    If Not bExists Then
                        WriteToErrorLog("readDeviceList(): Added Device " & sDevice.sDeviceId & " - " & sDevice.sTopic, 1)
                        oDevices.Add(sDevice)
                    End If
                End If
            End While
            fileIn.Close()
            For Each cDevice In oDevices
                If Not cDevice.bFoundInConfig Then
                    WriteToErrorLog("readDeviceList(): Removing Device " & cDevice.sDeviceId & " - " & cDevice.sTopic, 1)
                    oDevices.Remove(cDevice)
                End If
            Next
        Catch ex As Exception
            WriteToErrorLog("readDeviceList(): " & Err.Description)
        End Try
    End Sub

    Private Function getDeviceIdByTopic(inTopic As String) As List(Of clsDevice)
        getDeviceIdByTopic = New List(Of clsDevice)
        Try
            Dim sSplitTopic() As String
            Dim sDevice As String = ""

            sSplitTopic = Split(inTopic, "/")
            If UBound(sSplitTopic) > 0 Then
                sDevice = sSplitTopic(1)
            End If

            For Each device In oDevices
                If device.sTopic = sDevice Then
                    getDeviceIdByTopic.Add(device)
                End If
            Next
        Catch ex As Exception
            WriteToErrorLog("getDeviceIdByTopic(): " & Err.Description)
        End Try
    End Function

    Private Function getTopicDeviceByTopic(inTopic As String) As String
        Try
            Dim sSplitTopic() As String
            Dim sDevice As String = ""


            sSplitTopic = Split(inTopic, "/")
            If UBound(sSplitTopic) > 0 Then
                sDevice = sSplitTopic(1)
            End If
            getTopicDeviceByTopic = sDevice
        Catch ex As Exception
            WriteToErrorLog("getTopicDeviceByTopic(): " & Err.Description)
            Return ""
        End Try
    End Function

    Private Sub WriteToErrorLog(ByVal msg As String, Optional ByVal iLevel As Integer = 0)
        Try
            If iLevel > iLogLevel Then
                Exit Sub
            End If
            If gLogDir = "" Then
                Dim dir As String
                dir = System.IO.Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location)
                gLogDir = dir & "\logs"
                'gLogDir = "c:\SmartThingsMQTT\logs"
            End If

            Dim fs As System.IO.FileStream = New System.IO.FileStream(gLogDir & "\" & Format(Today(), "yyyyMMdd") & ".log", System.IO.FileMode.OpenOrCreate, System.IO.FileAccess.ReadWrite)
            Dim s As System.IO.StreamWriter = New System.IO.StreamWriter(fs)
            s.Close()
            fs.Close()

            Dim fs1 As System.IO.FileStream = New System.IO.FileStream(gLogDir & "\" & Format(Today(), "yyyyMMdd") & ".log", System.IO.FileMode.Append, System.IO.FileAccess.Write)
            Dim s1 As System.IO.StreamWriter = New System.IO.StreamWriter(fs1)
            s1.Write(DateTime.Now.ToString("T") & " : " & msg & vbCrLf)
            s1.Close()
            fs1.Close()

        Catch ex As Exception
            'Can't log
        End Try
    End Sub

    Private Async Function SendRequest(url As String, jsonString As String, inMessageGuid As Guid) As Task(Of Boolean)
        Try
            WriteToErrorLog("In:  SendRequest(" & url & ", " & jsonString, 3)
            Dim uri As Uri = New Uri(url)
            Dim req As WebRequest = WebRequest.Create(uri)
            Dim jsonDataBytes = Encoding.UTF8.GetBytes(jsonString)

            req.Headers.Add("Authorization", "Bearer: " & token)
            req.ContentType = "application/json"
            req.Method = "POST"
            req.ContentLength = jsonDataBytes.Length

            Using myStream As Stream = Await req.GetRequestStreamAsync()
                myStream.Write(jsonDataBytes, 0, jsonDataBytes.Length)
                myStream.Close()
                Using response As WebResponse = Await req.GetResponseAsync()
                    Using responseStream As Stream = response.GetResponseStream()
                    End Using
                End Using
            End Using
            WriteToErrorLog("Out:  SendRequest(" & url & ", " & jsonString, 3)
            updateMessageStatus(inMessageGuid)
            Return True
        Catch ex As Exception
            updateMessageStatus(inMessageGuid)
            WriteToErrorLog("SendRequest():  Error sending [" & url & "]")
            WriteToErrorLog("SendRequest(): " & Err.Description)
            Return False
        End Try
    End Function

    Private Async Function sendData(inDevice As String, inData As String, inMessageGuid As Guid) As Task(Of Boolean)
        Try
            WriteToErrorLog("In:  sendData(" & inDevice & ", " & inData, 3)
            If inDevice = "" Then
                updateMessageStatus(inMessageGuid)
                Return True
                Exit Function
            End If
            If IsValidJson(inData) Then
                Dim jSonString As String = "{'commands':  [{'component' :  'main','capability': 'execute','command': 'execute','arguments': ['" & inData & "']}]}"
                Dim url As String = "https://api.smartthings.com/v1/devices/" & inDevice & "/commands"
                Await SendRequest(url, jSonString, inMessageGuid)
            Else
                updateMessageStatus(inMessageGuid)
            End If
            WriteToErrorLog("Out:  sendData(" & inDevice & ", " & inData, 3)
            Return True
        Catch ex As Exception
            updateMessageStatus(inMessageGuid)
            WriteToErrorLog("SendData: " & Err.Description)
            Return False
        End Try
    End Function


End Class