Imports System.Threading
Imports MQTTnet
Imports MQTTnet.Server
Imports System.IO
Imports System.Net
Imports System.Text
Imports Newtonsoft.Json.Linq
Imports Newtonsoft.Json
Imports System.Collections.Concurrent

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

    Private Class tMessage
        Public sDevice As String
        Public sData As String
    End Class

    Private Class clsDevice
        Public sDeviceId As String
        Public sTopic As String
        Public bFoundInConfig As Boolean
    End Class

    Dim tQueue As ConcurrentQueue(Of tMessage) = New ConcurrentQueue(Of tMessage)()

    Protected Overrides Sub OnStart(ByVal args() As String)
        Try
            WriteToErrorLog("OnStart(): IN", 3)
            Dim objServerOptions As New MqttServerOptions()
            Dim gAppDir = My.Application.Info.DirectoryPath
            gLogDir = gAppDir
            myServer = myMQTT.CreateMqttServer()
            objServerOptions.EnablePersistentSessions = True
            myServer.UseApplicationMessageReceivedHandler(AddressOf OnApplicationMessageReceived)
            myServer.StartAsync(objServerOptions)
            readConfig()
            readDeviceList()
            AddHandler TimerConfig.Elapsed, AddressOf TimerX_Tick
            With TimerConfig
                .Interval = 60000
                .Enabled = True
            End With


            Dim evaluator As New Thread(Sub() processQueue())
            With evaluator
                .IsBackground = True ' not necessary...
                .Start()
            End With

            WriteToErrorLog("OnStart(): OUT", 3)
        Catch ex As Exception
            WriteToErrorLog("OnStart()" & Err.Description)
        End Try

    End Sub

    Private Sub TimerX_Tick(ByVal sender As System.Object, ByVal e As System.EventArgs)
        readConfig()
        readDeviceList()
    End Sub

    Async Sub processQueue()
        While Not bStopping
            Try
                Dim lMessage As New tMessage
                If tQueue.TryDequeue(lMessage) Then
                    'WriteToErrorLog("Processing " & lMessage.sDevice & "-" & lMessage.sData)
                    Await sendData(lMessage.sDevice, lMessage.sData)
                Else
                End If
            Catch ex As Exception
                WriteToErrorLog("processQueue(): " & Err.Description)
            End Try
            'WriteToErrorLog("Still Going")
        End While
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


    Sub OnApplicationMessageReceived(ByVal eventArgs As MqttApplicationMessageReceivedEventArgs)
        Try
            Dim sTopic = eventArgs.ApplicationMessage.Topic
            Dim sPayload = UnicodeBytesToString(eventArgs.ApplicationMessage.Payload)
            Dim sValue = eventArgs.ApplicationMessage.ToString
            Dim sTopicDevice As String = ""
            Dim lMessage As New tMessage

            sTopicDevice = getTopicDeviceByTopic(sTopic)
            If sPayload = "Offline" Then
                WriteToErrorLog("Payload:  " & sTopicDevice & " - [" & sPayload & "]", 1)
            Else
                If bLogAllMessages Then
                    WriteToErrorLog("Payload:  " & sTopicDevice & " - [" & sPayload & "]", 2)
                End If
            End If

            For Each device In getDeviceIdByTopic(sTopic)
                lMessage = New tMessage
                lMessage.sDevice = device.sDeviceId
                lMessage.sData = sPayload
                tQueue.Enqueue(lMessage)
            Next

        Catch ex As Exception
            WriteToErrorLog("OnApplicationMessageReceived():  " & Err.Description)
        End Try
    End Sub

    Private Function UnicodeBytesToString(
    ByVal bytes() As Byte) As String
        Try
            Return System.Text.Encoding.ASCII.GetString(bytes)
        Catch ex As Exception
            WriteToErrorLog("UnicodeBytesToString(): " & Err.Description, 1)
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
                    lineInfo = Split(strData, "=")
                    Select Case lineInfo(0).ToLower
                        Case "smartthingstoken"
                            token = lineInfo(1)
                        Case "loglevel"
                            iLogLevel = strToInteger(lineInfo(1))
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
            Dim dir As String
            dir = System.IO.Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location)
            gLogDir = dir & "\logs"
            'gLogDir = "c:\SmartThingsMQTT\logs"

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

    Private Async Function SendRequest(url As String, jsonString As String) As Task(Of Boolean)
        Try
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

            Return True
        Catch ex As Exception
            WriteToErrorLog("SendRequest():  Error sending [" & url & "]")
            WriteToErrorLog("SendRequest(): " & Err.Description)
            Return False
        End Try
    End Function

    Private Async Function sendData(inDevice As String, inData As String) As Task(Of Boolean)
        Try
            If inDevice = "" Then
                Return True
                Exit Function
            End If
            If IsValidJson(inData) Then
                Dim jSonString As String = "{'commands':  [{'component' :  'main','capability': 'execute','command': 'execute','arguments': ['" & inData & "']}]}"
                Dim url As String = "https://api.smartthings.com/v1/devices/" & inDevice & "/commands"
                Await SendRequest(url, jSonString)
            End If
            Return True
        Catch ex As Exception
            WriteToErrorLog("SendData: " & Err.Description)
            Return False
        End Try
    End Function


End Class