//iot_websocket app
//D:\codevs\springboot\iot_folder\iot_websocket\iot_websocket_control_remote\iot_websocket_control_remote.ino
#include <ESP8266WiFi.h>
#include <WebSocketsClient.h>
#include <ArduinoJson.h>

// Initialize WebSocket client
WebSocketsClient webSocket;

// Define motor control pins
#define ENA 5    // GPIO5 (D1)
#define ENB 12   // GPIO12 (D6)
#define IN1 4    // GPIO4 (D2)
#define IN2 0    // GPIO0 (D3)
#define IN3 2    // GPIO2 (D4)
#define IN4 14   // GPIO14 (D5)

// Define Wi-Fi credentials
const char* ssid = "DESKTOP-G2I50NN 9515";    // SSID bạn muốn kết nối
const char* password = "66666666";           // Mật khẩu Wi-Fi
const char* websocket_server="wss://messagingstompwebsocket-latest.onrender.com:443/ws";

// Control motor speed (0-1023)
const int MOTOR_SPEED = 800;

// Define LED pin
const int ledPin = D2; // GPIO4 (D2)

// Define Battery pin (nếu sử dụng cảm biến pin)
#define BATTERY_PIN A0 // Thay thế A0 bằng chân ADC thực tế của bạn

// Variables for status update timing
unsigned long lastStatusUpdate = 0;
const unsigned long STATUS_INTERVAL = 5000; // 5000 ms = 5 giây

// Variable to track previous connection status
bool wasConnected = false;

// Function Prototypes
void pinInit();
bool wifiInit();
bool websocketInit();
void sendStatus();
void webSocketEvent(WStype_t type, uint8_t * payload, size_t length);
void handleMoveRequest(const char* request);
void tien();
void lui();
void dung();
void retrai();
void rephai();
int getBatteryLevel();

void setup() {
    Serial.begin(115200); // Sử dụng baud rate 115200
    Serial.println("\n===== Starting Setup =====");

    pinInit();

    if(!wifiInit()) {
        Serial.println("!!! Wi-Fi Initialization Failed !!!");
    }

    if(!websocketInit()) {
        Serial.println("!!! WebSocket Initialization Failed !!!");
    }

    Serial.println("\n***___________________^^__________________^^__________________*** WELCOME TO CONTROL CAR REMOTE ***___________________^^__________________^^__________________***");
}

void loop() {
    webSocket.loop();

    unsigned long currentMillis = millis();

    // Cập nhật trạng thái mỗi 5 giây chỉ khi WebSocket kết nối
    if ((currentMillis - lastStatusUpdate >= STATUS_INTERVAL)&&(!WiFi.status() == WL_CONNECTED)
    // && (!webSocket.isConnected())
    ) {
      ESP.restart();
        // sendStatus();
        lastStatusUpdate = currentMillis;
    }

    // Thêm các chức năng khác nếu cần
}

// Function to initialize motor control pins
void pinInit(){
    pinMode(ENA, OUTPUT);
    pinMode(IN1, OUTPUT);
    pinMode(IN2, OUTPUT);
    pinMode(ENB, OUTPUT);
    pinMode(IN3, OUTPUT);
    pinMode(IN4, OUTPUT);
    pinMode(ledPin, OUTPUT); // Initialize LED pin

    // Initialize motors and LED to OFF state
    digitalWrite(ENA, LOW);
    digitalWrite(IN1, LOW);
    digitalWrite(IN2, LOW);
    digitalWrite(ENB, LOW);
    digitalWrite(IN3, LOW);
    digitalWrite(IN4, LOW);
    digitalWrite(ledPin, LOW); // Turn off LED

    Serial.println("[Initialization] Motor pins initialized to LOW.");
}

// Function to initialize Wi-Fi connection
bool wifiInit(){
    Serial.print("[Wi-Fi] Connecting to SSID: ");
    Serial.println(ssid);

    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, password);
    Serial.print("[Wi-Fi] Connecting to Wi-Fi");

    unsigned long startAttemptTime = millis();

    // Cố gắng kết nối đến Wi-Fi trong 10 giây
    while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < 10000) {
        delay(500);
        Serial.print(".");
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\n[Wi-Fi] Connected Successfully");
        Serial.print("[Wi-Fi] IP Address: ");
        Serial.println(WiFi.localIP());
        return true;
    } else {
        Serial.println("\n[Wi-Fi] Failed to Connect");
        Serial.println("[Wi-Fi] Restarting ESP8266 in 5 seconds...");
        delay(5000);
        ESP.restart();
        return false;
    }
}

// Function to initialize WebSocket connection
bool websocketInit(){
    Serial.println("[WebSocket] Initializing WebSocket connect to wss://messagingstompwebsocket-latest.onrender.com:443/move");
    //messagingstompwebsocket-latest.onrender.com:443/move
    // webSocket.begin("192.168.0.104", 8060, "/ws"); // Sử dụng địa chỉ mạng LAN IPv4 thực tế máy chạy spring socket server
    webSocket.beginSSL("messagingstompwebsocket-latest.onrender.com", 443, "/move");//gọi kết nối tới wss://messagingstompwebsocket-latest.onrender.com:443/ws qua extension piesocket với tin nhắn {"action_move_name":"BACKWARD","speed":10}
  //  webSocket.begin("messagingstompwebsocket-latest.onrender.com", 443, "/ws", "wss");
  //  webSocket.begin(websocket_server);

    webSocket.onEvent(webSocketEvent);
    webSocket.setReconnectInterval(5000); // Reconnect interval set to 5 giây
    webSocket.enableHeartbeat(15000, 3000, 2); // Ping mỗi 15 giây, timeout 3 giây, gửi lại 2 lần nếu timeout
    Serial.println("[WebSocket] Initialization Complete.");
    return true;
}

// Function to send status information
void sendStatus() {
    // Kiểm tra kết nối Wi-Fi
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("[Status Update] Cannot send status: Not connected to Wi-Fi");
        // return;
    }
    // Kiểm tra kết nối WebSocket
    if (!webSocket.isConnected()) {
        Serial.println("[Status Update] Cannot send status: WebSocket not connected");
        // return;
    }

    int wifiSignalStrength = WiFi.RSSI(); // Get Wi-Fi signal strength

    // Construct JSON payload
    StaticJsonDocument<200> doc;
    doc["wifiStatus"] = "connected";
    doc["wifiSignalStrength"] = wifiSignalStrength;
    // doc["batteryLevel"] = getBatteryLevel(); // Bỏ nếu không sử dụng

    String jsonStatus;
    serializeJson(doc, jsonStatus);

    Serial.println("[Status Update] Sending status: " + jsonStatus);
    webSocket.sendTXT(jsonStatus);
}

// WebSocket event handler
void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
    switch (type) {
        case WStype_DISCONNECTED:
            if (wasConnected) {
                Serial.println("[WebSocket] Disconnected");
                // wasConnected = false;
                // ESP.restart();
            }
            break;
        case WStype_CONNECTED:
            Serial.println("[WebSocket] Connected to server");
            // Gửi tin nhắn JSON ngay sau khi kết nối
            String message = "esp8266 device connected";
            webSocket.sendTXT(message);
            wasConnected = true;
            break;
        case WStype_TEXT:
            Serial.println("[WebSocket] Received text: " + String((char*)payload));
            handleMoveRequest((char*)payload);
            break;
        case WStype_ERROR:
            Serial.println("[WebSocket] Error occurred");
            break;
        case WStype_PING:
            // Ping từ server
            Serial.println("[WebSocket] Ping received");
            break;
        case WStype_PONG:
            // Đã nhận được phản hồi từ việc ping đến server
            Serial.println("[WebSocket] Pong received");
            break;
        default:
            Serial.printf("[WebSocket] Unknown event type: %d\n", type);
            break;
    }
}

// Function to handle move requests from the server
void handleMoveRequest(const char* request) {
    Serial.println("[Move] Handling move request...");

    // // Parse JSON request
    // StaticJsonDocument<200> doc;
    // DeserializationError error = deserializeJson(doc, request);

     // Chuyển payload thành chuỗi String
    String payloadStr = String(request);

    // Kiểm tra xem chuỗi có bắt đầu và kết thúc với dấu ngoặc nhọn hay không
    if (!payloadStr.startsWith("{")) {
        payloadStr = "{" + payloadStr; // Thêm dấu ngoặc nhọn mở nếu thiếu
    }
    if (!payloadStr.endsWith("}")) {
        payloadStr = payloadStr + "}"; // Thêm dấu ngoặc nhọn đóng nếu thiếu
    }

    // Khởi tạo đối tượng JSON
    DynamicJsonDocument doc(1024);

    // Giải mã chuỗi JSON
    DeserializationError error = deserializeJson(doc, payloadStr);
    if (error) {
        Serial.print("[Move] deserializeJson() failed: ");
        Serial.println(error.f_str());
        return;
    }
    // Kiểm tra xem các trường JSON có tồn tại hay không
    if (!doc.containsKey("action_move_name") || !doc.containsKey("speed")) {
        Serial.println("[Move] Invalid JSON: Missing fields");
        return;
    }

    const char* move = doc["action_move_name"];
    long speed = doc["speed"];

     // Kiểm tra giá trị null hoặc bất thường
    if (move == nullptr) {
        Serial.println("[Move] Invalid move command");
        return;
    }

    Serial.printf("[Move] Received: %s, Speed: %ld\n", move, speed);

    // Execute move based on the command
    if (strcmp(move, "tien") == 0) {
        tien();
    } else if (strcmp(move, "lui") == 0) {
        lui();
    } else if (strcmp(move, "dung") == 0) {
        dung();
    } else if (strcmp(move, "retrai") == 0) {
        retrai();
    } else if (strcmp(move, "rephai") == 0) {
        rephai();
    } else {
        Serial.println("[Move] Unknown move received");
        // Gửi phản hồi lỗi nếu cần
        StaticJsonDocument<200> responseDoc;
        responseDoc["status"] = "ERROR";
        responseDoc["message"] = "Unknown move: " + String(move);

        String response;
        serializeJson(responseDoc, response);
        webSocket.sendTXT(response);
        return;
    }

    // Respond back to the server
    StaticJsonDocument<200> responseDoc;
    responseDoc["status"] = "SUCCESS";
    responseDoc["move"] = move;
    responseDoc["speed"] = speed;

    String response;
    serializeJson(responseDoc, response);
    Serial.println("[Move] Sending response: " + response);
    webSocket.sendTXT(response);
}

// Function to control forward movement
void tien() {
    Serial.println("[Move] Executing 'tien' (forward).");
    digitalWrite(IN1, LOW);
    digitalWrite(IN2, HIGH);
    analogWrite(ENA, MOTOR_SPEED);
    digitalWrite(IN3, LOW);
    digitalWrite(IN4, HIGH);
    analogWrite(ENB, MOTOR_SPEED + 300);
    digitalWrite(ledPin, HIGH); // Turn on LED
    Serial.println("[Move] 'tien' executed.");
}

// Function to control backward movement
void lui() {
    Serial.println("[Move] Executing 'lui' (backward).");
    digitalWrite(IN1, HIGH);
    digitalWrite(IN2, LOW);
    analogWrite(ENA, MOTOR_SPEED);
    digitalWrite(IN3, HIGH);
    digitalWrite(IN4, LOW);
    analogWrite(ENB, MOTOR_SPEED + 300);
    digitalWrite(ledPin, LOW); // Turn off LED
    Serial.println("[Move] 'lui' executed.");
}

// Function to stop movement
void dung() {
    Serial.println("[Move] Executing 'dung' (stop).");
    digitalWrite(IN1, LOW);
    digitalWrite(IN2, LOW);
    analogWrite(ENA, 0);
    digitalWrite(IN3, LOW);
    digitalWrite(IN4, LOW);
    analogWrite(ENB, 0);
    digitalWrite(ledPin, LOW); // Turn off LED
    Serial.println("[Move] 'dung' executed.");
}

// Function to turn left
void retrai() {
    Serial.println("[Move] Executing 'retrai' (turn left).");
    digitalWrite(IN1, LOW);
    digitalWrite(IN2, LOW);
    analogWrite(ENA, MOTOR_SPEED);
    digitalWrite(IN3, LOW);
    digitalWrite(IN4, HIGH);
    analogWrite(ENB, MOTOR_SPEED);
    digitalWrite(ledPin, HIGH); // Turn on LED
    Serial.println("[Move] 'retrai' executed.");
}

// Function to turn right
void rephai() {
    Serial.println("[Move] Executing 'rephai' (turn right).");
    digitalWrite(IN1, LOW);
    digitalWrite(IN2, HIGH);
    analogWrite(ENA, MOTOR_SPEED);
    digitalWrite(IN3, LOW);
    digitalWrite(IN4, LOW);
    analogWrite(ENB, MOTOR_SPEED);
    digitalWrite(ledPin, HIGH); // Turn on LED
    Serial.println("[Move] 'rephai' executed.");
}

// Function to get battery level (bỏ nếu không sử dụng)
int getBatteryLevel() {
    // Replace with actual battery level reading logic
    // Example: Reading from analog pin and converting to percentage
    int sensorValue = analogRead(BATTERY_PIN);
    float voltage = sensorValue * (3.3 / 1023.0); // Adjust based on your voltage divider
    int batteryPercent = (voltage / 3.3) * 100; // Assuming 3.3V is 100%
    batteryPercent = constrain(batteryPercent, 0, 100);
    Serial.printf("[Battery] Battery Level: %d%%\n", batteryPercent);
    return batteryPercent;
}
