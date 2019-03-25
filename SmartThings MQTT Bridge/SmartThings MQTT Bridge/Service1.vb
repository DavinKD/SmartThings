Imports System.Threading
Imports MQTTnet
Imports MQTTnet.Server
Imports System.IO
Imports System.Net
Imports System.Text
Imports Newtonsoft.Json.Linq
Imports Newtonsoft.Json

Public Class SmartThingsMQTTService1

    Dim WithEvents myMQTT As New MQTTnet.MqttFactory
    Dim WithEvents myServer As IMqttServer
    Dim token As String = ""
    Dim oDevices As New List(Of clsDevice)
    Dim gLogDir As String = ""

    Private Class clsDevice
        Public sDeviceId As String
        Public sTopic As String
    End Class


    Protected Overrides Sub OnStart(ByVal args() As String)
        Try
            Dim objServerOptions As New MqttServerOptions()
            Dim gAppDir = My.Application.Info.DirectoryPath
            gLogDir = gAppDir
            WriteToErrorLog("onStart():  In")
            myServer = myMQTT.CreateMqttServer()
            myServer.StartAsync(objServerOptions)
            readConfig()
            readDeviceList()

        Catch ex As Exception
            WriteToErrorLog("OnStart()" & Err.Description)
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

            myServer.StopAsync()
        Catch ex As Exception
            WriteToErrorLog("OnStop(): " & Err.Description)
        End Try
    End Sub

    Private Sub OnApplicationMessageReceived(ByVal sender As Object, ByVal eventArgs As MqttApplicationMessageReceivedEventArgs) Handles myServer.ApplicationMessageReceived
        Try
            Dim sTopic = eventArgs.ApplicationMessage.Topic
            Dim sPayload = UnicodeBytesToString(eventArgs.ApplicationMessage.Payload)
            Dim sValue = eventArgs.ApplicationMessage.ToString
            Dim sTopicDevice As String = ""

            sTopicDevice = getTopicDeviceByTopic(sTopic)
            WriteToErrorLog("Payload:  " & sTopicDevice & " - [" & sPayload & "]")
            For Each device In getDeviceIdByTopic(sTopic)
                Dim evaluator As New Thread(Sub() Me.sendData(device.sDeviceId, sPayload))
                With evaluator
                    .IsBackground = True ' not necessary...
                    .Start()
                End With

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
            WriteToErrorLog("UnicodeBytesToString(): " & Err.Description)
            Return ""
        End Try
    End Function

    Private Sub readConfig()
        Try
            Dim path1 As String = "C:\SmartThingsMQTT\SmartThingsMQTT.cfg"
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
                    End Select
                End If
            End While
        Catch ex As Exception
            WriteToErrorLog("ReadConfig(): " & Err.Description)
        End Try
    End Sub
    Private Sub readDeviceList()
        Try
            Dim path1 As String = "C:\SmartThingsMQTT\deviceList.cfg"

            Dim fileIn As New StreamReader(path1)
            Dim lineInfo(2) As String
            Dim strData As String
            Dim sDevice As New clsDevice

            While Not (fileIn.EndOfStream)
                sDevice = New clsDevice
                strData = fileIn.ReadLine
                If Trim(strData) <> "" Then
                    lineInfo = Split(strData, "=")
                    sDevice.sTopic = lineInfo(0)
                    sDevice.sDeviceId = lineInfo(1)
                    oDevices.Add(sDevice)
                End If
            End While
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

    Private Sub WriteToErrorLog(ByVal msg As String)
        Try
            gLogDir = "c:\SmartThingsMQTT\logs"

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

    Private Function SendRequest(url As String, jsonString As String) As String
        Try
            Dim uri As Uri = New Uri(url)
            Dim req As WebRequest = WebRequest.Create(uri)
            Dim jsonDataBytes = Encoding.UTF8.GetBytes(jsonString)

            req.Headers.Add("Authorization", "Bearer: " & token)
            req.ContentType = "application/json"
            req.Method = "POST"
            req.ContentLength = jsonDataBytes.Length




            Dim stream = req.GetRequestStream()
            stream.Write(jsonDataBytes, 0, jsonDataBytes.Length)
            stream.Close()

            Dim response = req.GetResponse().GetResponseStream()

            Dim reader As New StreamReader(response)
            Dim res = reader.ReadToEnd()
            reader.Close()
            response.Close()

            Return res
        Catch ex As Exception
            WriteToErrorLog("SendRequest(): " & Err.Description)
            Return ""
        End Try
    End Function

    Private Function sendData(inDevice As String, inData As String) As Boolean
        Try
            If inDevice = "" Then
                'WriteToErrorLog("sendData():  Invalid device")
                sendData = False
                Exit Function
            End If
            If IsValidJson(inData) Then
                Dim jSonString As String = "{'commands':  [{'component' :  'main','capability': 'execute','command': 'execute','arguments': ['" & inData & "']}]}"
                Dim url As String = "https://api.smartthings.com/v1/devices/" & inDevice & "/commands"
                Dim result_post = SendRequest(url, jSonString)
                sendData = True
            Else
                sendData = False
            End If
        Catch ex As Exception
            WriteToErrorLog("SendData: " & Err.Description)
            sendData = False
        End Try
    End Function


End Class
