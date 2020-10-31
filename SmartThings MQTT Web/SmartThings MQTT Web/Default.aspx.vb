Imports System.Threading
Imports MQTTnet
Imports MQTTnet.Server
Imports MQTTnet.Client
Imports System.IO
Imports System.Net
Imports System.Text
Imports Newtonsoft.Json.Linq
Imports Newtonsoft.Json
Public Class _Default
    Inherits Page
    Dim myMQTT As New MQTTnet.MqttFactory
    Dim myClient As MQTTnet.Client.IMqttClient

    Protected Sub Page_Load(ByVal sender As Object, ByVal e As EventArgs) Handles Me.Load
        Dim sTopic As String = ""
        Dim sPayload As String = ""
        Dim lCount As Integer = 0
        sTopic = Request("topic")
        sPayload = Request("payload")

        myClient = myMQTT.CreateMqttClient()
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

    End Sub
End Class