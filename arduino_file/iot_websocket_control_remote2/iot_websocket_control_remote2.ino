//iot_websocket app
//D:\codevs\springboot\iot_folder\iot_websocket\iot_websocket_control_remote\iot_websocket_control_remote2.ino
#include <ESP8266WiFi.h>
#include <WebSocketsClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <stdarg.h>
//lỗi
// Initialize WebSocket client
WebSocketsClient webSocket;

// Cấu hình cảm biến DHT
#define DHTPIN D1       // Chân DATA của DHT nối với GPIO 1 (D1 trên NodeMCU)
#define DHTTYPE DHT11  // Dịnh nghĩa loại DHT là 11 Hoặc thay bằng DHT22 nếu dùng DHT22
DHT dht(DHTPIN, DHTTYPE);

// Cấu hình cảm biến khí gas (MQ-2)
#define MQ2_PIN D7     // Chân AO của MQ-2 nối với D7 trên NodeMCU

// Define motor control pins
#define ENA 5   // GPIO5 (D1)
#define ENB 12  // GPIO12 (D6)
#define IN1 4   // GPIO4 (D2)
#define IN2 0   // GPIO0 (D3)
#define IN3 2   // GPIO2 (D4)
#define IN4 14  // GPIO14 (D5)

unsigned long lastStatusUpdate=0;//khởi tạo trước biến đo thời gian trong 
// Biến để lưu giá trị đọc từ cảm biến
float temperature = 0.0;
float humidity = 0.0;
int gasValue = 0;

// Define Wi-Fi credentials
const char* ssid = "DESKTOP-G2I50NN 9515";  // SSID bạn muốn kết nối
const char* password = "66666666";          // Mật khẩu Wi-Fi
const char* websocket_server = "wss://messagingstompwebsocket-latest.onrender.com:443/ws";

// Control motor speed (0-1023)
const int MOTOR_SPEED = 800;

// Define LED pin
const int ledPin = D2;  // GPIO4 (D2)

// Define Battery pin (nếu sử dụng cảm biến pin)
#define BATTERY_PIN A0  // Thay thế A0 bằng chân ADC thực tế của bạn

// Variables for status update timing
const unsigned long STATUS_INTERVAL = 5000;  // 5000 ms = 5 giây

// Function Prototypes
void sendwifiSignalStrength();
void sendTemperatureAndHumidityDataFromDHT();
void sendGasData();
void sendStatus();
void pinInit();
// void tryWiFiConnect();
void websocketInit();
void webSocketEvent(WStype_t type, uint8_t* payload, size_t length);
void handleMoveRequest(const char* request);
void tien();
void lui();
void retrai();
void rephai();
void dung();
int getBatteryLevel();

void setup() {
  Serial.begin(115200);  // Sử dụng baud rate 115200
  Serial.println("\n===== Starting Setup =====");
  // pinInit();
  // tryWiFiConnect();
  // websocketInit();
  // Serial.println("\n***___________________^^__________________^^__________________*** WELCOME TO CONTROL CAR REMOTE ***___________________^^__________________^^__________________***");
}

void loop() {
  // lastStatusUpdate = millis();  // lưu thời gian lần kiểm tra mới nhất
  // if ((millis() - lastStatusUpdate >= 5000) && (WiFi.status() != WL_CONNECTED)) {// Cập nhật trạng thái khởi tạo đảm bảo chức năng cần thiết (kết nối wifi) cho esp hoạt động bình thường mỗi 5 giây
  //   lastStatusUpdate = millis();
  //   tryWiFiConnect();
  // } 
  // delay(1000);
  // webSocket.loop(); // Duy trì kết nối WebSocket
  // sendStatus();// Cập nhật trạng thái cảm biến lên server
}
// Hàm toJson nhận các cặp key-value động và lưu giá trị mặc định là string
String toJson(int numPairs, ...) {
  StaticJsonDocument<1024> doc; // Kích thước JSON tài liệu có thể điều chỉnh
  va_list args;
  va_start(args, numPairs);

  for (int i = 0; i < numPairs; i++) {
    const char* key = va_arg(args, const char*);
    const char* value = va_arg(args, JsonVariant);
    // Lưu giá trị dưới dạng chuỗi
    doc[key] = value;
  }
  va_end(args);
  // Serialize JSON thành chuỗi
  String jsonString;
  serializeJson(doc, jsonString);
  return jsonString;
}
void sendStatus(){
  sendwifiSignalStrength();
  sendTemperatureAndHumidityDataFromDHT();// gửi dữ liệu của DHT11
  sendGasData(); // gửi dữ liệu khí gas
}
void sendTemperatureAndHumidityDataFromDHT() {//gửi nhiệt độ và độ ẩm
  float temperature = dht.readTemperature();// đọc nhiệt độ
  float humidity = dht.readHumidity();       // Đọc độ ẩm

  if (isnan(temperature) || isnan(humidity) ) {
    Serial.println("[Error] Lỗi đọc dữ liệu nhiệt độ từ cảm biến DHT11");
    return;
  }
   // In kết quả ra Serial Monitor
  // Serial.print("Nhiệt độ: ");
  // Serial.print(temperature);
  // Serial.println(" °C");
  // Serial.print("Độ ẩm: ");
  // Serial.print(humidity);
  // Serial.println(" %");

  // Gửi JSON qua WebSocket
  String DHT11Json = toJson(
      2,
      "temperature_data",
       temperature, // Chuyển nhiệt độ sang chuỗi
       "humidity_data",
       humidity
  );
  webSocket.sendTXT(DHT11Json);
  Serial.println("[DHT11Json] Đã gửi: " + DHT11Json);
}
void sendGasData() {
  int gasValue = analogRead(MQ2_PIN); // Đọc giá trị từ chân analog của cảm biến MQ-2

  if (gasValue < 0) {
    Serial.println("[Error] Lỗi đọc dữ liệu khí gas từ cảm biến MQ-2!");
    return;
  }

  // Tạo JSON để gửi
  String gasJson = toJson(
      1,                       // Số cặp key-value trong JSON
      "gas_data",             // Key
      gasValue // Value, chuyển giá trị khí gas sang chuỗi
  );

  // Gửi JSON qua WebSocket
  webSocket.sendTXT(gasJson);

  // Ghi log ra Serial để kiểm tra
  Serial.println("[Gas Data] Đã gửi: " + gasJson);
}



// Function to initialize motor control pins
void pinInit() {
  Serial.println("Khởi động cảm biến DHT11!");
  dht.begin();// bắt đầu dht
  pinMode(ENA, OUTPUT);
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(ENB, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);
  pinMode(ledPin, OUTPUT);  // Initialize LED pin

  // Initialize motors and LED to OFF state
  digitalWrite(ENA, LOW);
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  digitalWrite(ENB, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
  digitalWrite(ledPin, LOW);  // Turn off LED

  Serial.println("[Initialization] Motor pins initialized to LOW.");
}

// Function to initialize Wi-Fi connection
void tryWiFiConnect() {
  //WiFi.status() WL_IDLE_STATUS,WL_NO_SSID_AVAIL,WL_SCAN_COMPLETED,WL_CONNECTED,WL_CONNECT_FAILED,WL_CONNECTION_LOST,WL_DISCONNECTED
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }
  Serial.println(String("[Wi-Fi] Connect to SSID: ") + ssid + " ,PASS: " + password + " ,mode WIFI_STA");
  Serial.print(String("[Wi-Fi] Connecting"));
  WiFi.mode(WIFI_STA);  //WIFI_OFF-tắt,WIFI_STA-thiết bị khách kết nối tới wifi khác,WIFI_AP-tạo điểm truy cập,WIFI_AP_STA-cả 2 chế độ
  WiFi.begin(ssid, password);
  unsigned long startAttemptTime = millis();  //giữ thời gian trước khi kết nối millis()-lấy thời gian hiện tại
  // Cố gắng kết nối đến Wi-Fi trong 10 giây
  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < 10000) {
    delay(500);
    Serial.print(".");
  }
  if (WiFi.status() == !WL_CONNECTED) {
      Serial.println(String("\n[Wi-Fi] Failed to Connect with status: ")+WiFi.status());
      Serial.println("[Wi-Fi] Restarting ESP8266 in 5 seconds...");
      delay(5000);
      // ESP.restart();
  }else {
    Serial.println("\n[Wi-Fi] try Connect Successfully");
    Serial.print("[Wi-Fi] IP Address: ");
    Serial.println(WiFi.localIP());
  } 
}

// Function to initialize WebSocket connection
void websocketInit() {
  Serial.println("[WebSocket] Initializing WebSocket connect to ws://192.168.0.104:8060/move");
  // Serial.println("[WebSocket] Initializing WebSocket connect to wss://messagingstompwebsocket-latest.onrender.com:443/move");// webSocket.begin("192.168.0.104", 8060, "/ws"); // Sử dụng địa chỉ mạng LAN IPv4 thực tế máy chạy spring socket server
  // webSocket.beginSSL("messagingstompwebsocket-latest.onrender.com", 443, "/move");  //gọi kết nối tới wss://messagingstompwebsocket-latest.onrender.com:443/ws qua extension piesocket với tin nhắn {"action_move_name":"BACKWARD","speed":10}
  webSocket.begin("192.168.0.104", 8060, "/move");
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(5000);       // Reconnect interval set to 5 giây
  webSocket.enableHeartbeat(15000, 3000, 2);  // Ping mỗi 15 giây, timeout 3 giây, gửi lại 2 lần nếu timeout
  Serial.println("[WebSocket] Initialization Complete.");
}

// Function to send status information
void sendwifiSignalStrength() {
  // Serial.println("[Status Update] Sending status: " + jsonStatus);
  String status = (WiFi.status() == WL_CONNECTED) ? "Connected" : "Not Connected";
  String wifiSignalStrengthJson = toJson(
      2,
      "wifiStatus", status,
      "wifiSignalStrength",WiFi.RSSI()
    );
  webSocket.sendTXT(wifiSignalStrengthJson);
}

// WebSocket event handler
void webSocketEvent(WStype_t type, uint8_t* payload, size_t length) {
  String responseDeviceConnectedJson;
  switch (type) {
    case WStype_DISCONNECTED:
      Serial.println("[WebSocket] Disconnected");
      break;
    case WStype_CONNECTED:
      Serial.println("[WebSocket] Connected to server");
      // Gửi tin nhắn JSON ngay sau khi kết nối
       // Respond back to the server
      // Serial.println("Sending response: " + response);
      responseDeviceConnectedJson = toJson(  
        2,
        "status","SUCCESS",
        "message","esp8266 device connected"
      );
      webSocket.sendTXT(responseDeviceConnectedJson);
      break;
    case WStype_TEXT:
      Serial.println(String("[WebSocket] Received text: ") + String((char*)payload));
      handleMoveRequest((char*)payload);
      break;
    case WStype_ERROR:
      Serial.println(String("[WebSocket] Error occurred"));
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
      Serial.printf("%s", String("[WebSocket] Unknown event type: %d\n").c_str(), type);
      break;
  }
}

// Function to handle move requests from the server
void handleMoveRequest(const char* request) {
  Serial.println("[Move] Handling move request...");
  // Chuyển payload thành chuỗi String
  String payloadStr = String(request);
  // Kiểm tra xem chuỗi có bắt đầu và kết thúc với dấu ngoặc nhọn hay không, các tin nhắn dạng payload tới esp thiếu {}
  if (!payloadStr.startsWith("{")) {
    payloadStr = "{" + payloadStr;  // Thêm dấu ngoặc nhọn mở nếu thiếu
  }
  if (!payloadStr.endsWith("}")) {
    payloadStr = payloadStr + "}";  // Thêm dấu ngoặc nhọn đóng nếu thiếu
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
  if (move == nullptr || strlen(move) == 0) {
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
    String errorMoveControlJson = toJson(
        2,
      "status","ERROR",
      "message",String("Unknown move: ") + move
    );
    webSocket.sendTXT(errorMoveControlJson);
    return;
  }
  
  String successMoveControlJson = toJson(
    2,
    "status","SUCCESS",
    "message",String("move: ") + move + " ; speed: " + speed
  );
  webSocket.sendTXT(successMoveControlJson);
  // Respond back to the server
  Serial.println(String("[Move] Sending response: ") + successMoveControlJson);
}
// Function to control forward movement
void tien() {
  Serial.println("[Move] Executing 'tien' (forward).");
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  analogWrite(ENA, MOTOR_SPEED);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  analogWrite(ENB, MOTOR_SPEED + 300);
  digitalWrite(ledPin, HIGH);  // Turn on LED
  Serial.println("[Move] 'tien' executed.");
}
// Function to control backward movement
void lui() {
  Serial.println("[Move] Executing 'lui' (backward).");
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  analogWrite(ENA, MOTOR_SPEED);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  analogWrite(ENB, MOTOR_SPEED + 300);
  digitalWrite(ledPin, LOW);  // Turn off LED
  Serial.println("[Move] 'lui' executed.");
}

// Function to turn left
void retrai() {
  Serial.println("[Move] Executing 'retrai' (turn left).");
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  analogWrite(ENA, MOTOR_SPEED);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  analogWrite(ENB, MOTOR_SPEED);
  digitalWrite(ledPin, HIGH);  // Turn on LED
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
  digitalWrite(ledPin, HIGH);  // Turn on LED
  Serial.println("[Move] 'rephai' executed.");
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
  digitalWrite(ledPin, LOW);  // Turn off LED
  Serial.println("[Move] 'dung' executed.");
}

// Function to get battery level (bỏ nếu không sử dụng)
int getBatteryLevel() {
  // Replace with actual battery level reading logic
  // Example: Reading from analog pin and converting to percentage
  int sensorValue = analogRead(BATTERY_PIN);
  float voltage = sensorValue * (3.3 / 1023.0);  // Adjust based on your voltage divider
  int batteryPercent = (voltage / 3.3) * 100;    // Assuming 3.3V is 100%
  batteryPercent = constrain(batteryPercent, 0, 100);
  Serial.printf("[Battery] Battery Level: %d%%\n", batteryPercent);
  return batteryPercent;
}
