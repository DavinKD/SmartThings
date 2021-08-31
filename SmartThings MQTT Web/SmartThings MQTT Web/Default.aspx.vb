Imports System.Threading
Imports MQTTnet
Imports MQTTnet.Server
Imports MQTTnet.Client
Imports System.IO
Imports System.Net
Imports System.Text
Imports MQTTnet.Client.Options

Public Class _Default
    Inherits System.Web.UI.Page
    Dim myMQTT As New MQTTnet.MqttFactory
    Dim myClient As MQTTnet.Client.IMqttClient

    Protected Sub Page_Load(ByVal sender As Object, ByVal e As System.EventArgs) Handles Me.Load
        Try
            Dim sTopic As String = ""
            Dim sPayload As String = ""
            Dim lCount As Integer = 0
            sTopic = Request("topic")
            sPayload = Request("payload")
            WriteToErrorLog("IN: Topic[" & sTopic & "] Payload[" & sPayload & "]")

            myClient = myMQTT.CreateMqttClient()
            'Dim options As New MqttClientOptions

            Dim options = New MqttClientOptionsBuilder().WithTcpServer("127.0.0.1").Build()
            myClient.ConnectAsync(options)
            While Not myClient.IsConnected
                lCount += 1
                If lCount > 30 Then
                    Exit Sub
                End If
                Thread.Sleep(100)
            End While

            Dim message = New MqttApplicationMessageBuilder().WithTopic(sTopic).WithPayload(sPayload).Build()
            myClient.PublishAsync(message)
            myClient.DisconnectAsync()
            WriteToErrorLog("OUT: Topic[" & sTopic & "] Payload[" & sPayload & "]")
        Catch ex As Exception

        End Try

    End Sub
    Private Sub WriteToErrorLog(ByVal msg As String, Optional ByVal iLevel As Integer = 0)
        Try
            Dim gLogDir As String = ""
            Dim iLogLevel As Integer = 1
            If iLevel > iLogLevel Then
                Exit Sub
            End If
            Dim dir As String
            dir = System.IO.Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location)
            'gLogDir = dir & "\logs"
            gLogDir = "c:\SmartThingsMQTT\logs"

            Dim fs As System.IO.FileStream = New System.IO.FileStream(gLogDir & "\web" & Format(Today(), "yyyyMMdd") & ".log", System.IO.FileMode.OpenOrCreate, System.IO.FileAccess.ReadWrite)
            Dim s As System.IO.StreamWriter = New System.IO.StreamWriter(fs)
            s.Close()
            fs.Close()

            Dim fs1 As System.IO.FileStream = New System.IO.FileStream(gLogDir & "\web" & Format(Today(), "yyyyMMdd") & ".log", System.IO.FileMode.Append, System.IO.FileAccess.Write)
            Dim s1 As System.IO.StreamWriter = New System.IO.StreamWriter(fs1)
            s1.Write(DateTime.Now.ToString("T") & " : " & msg & vbCrLf)
            s1.Close()
            fs1.Close()

        Catch ex As Exception
            'Can't log
        End Try
    End Sub

End Class