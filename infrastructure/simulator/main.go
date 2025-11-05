package main

import (
	"encoding/json"
	"fmt"
	"log"
	"math"
	"math/rand"
	"os"
	"strconv"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

// Configuration from environment variables
type Config struct {
	MQTTBroker           string
	MQTTPort             string
	NumPatients          int
	EmitIntervalSeconds  int
	AlertIntervalSeconds int // Generate alert every N seconds
}

// Vital measurement types
type MeasurementType string

const (
	HeartRate      MeasurementType = "HEART_RATE"
	SpO2           MeasurementType = "SPO2"
	BloodPressure  MeasurementType = "BLOOD_PRESSURE"
	Glucose        MeasurementType = "GLUCOSE"
	Weight         MeasurementType = "WEIGHT"
	Steps          MeasurementType = "STEPS"
)

// VitalPayload represents the message sent to MQTT
type VitalPayload struct {
	PatientID       string                 `json:"patientId"`
	DeviceType      string                 `json:"deviceType"`
	MeasurementType MeasurementType        `json:"measurementType"`
	Value           float64                `json:"value"`
	Value2          *float64               `json:"value2,omitempty"` // For BP diastolic
	Unit            string                 `json:"unit"`
	Timestamp       string                 `json:"timestamp"`
	Metadata        map[string]interface{} `json:"metadata"`
}

// VitalConfig defines normal ranges and alert thresholds
type VitalConfig struct {
	Type           MeasurementType
	DeviceType     string
	Unit           string
	NormalMin      float64
	NormalMax      float64
	AlertMin       float64
	AlertMax       float64
	Value2Config   *Value2Config // For BP diastolic
}

type Value2Config struct {
	NormalMin float64
	NormalMax float64
	AlertMin  float64
	AlertMax  float64
}

var vitalConfigs = []VitalConfig{
	{
		Type:       HeartRate,
		DeviceType: "watch",
		Unit:       "bpm",
		NormalMin:  60,
		NormalMax:  100,
		AlertMin:   40,
		AlertMax:   150, // Changed from 110 to 150 to match Telegraf threshold
	},
	{
		Type:       SpO2,
		DeviceType: "watch",
		Unit:       "%",
		NormalMin:  95,
		NormalMax:  100,
		AlertMin:   85,
		AlertMax:   100,
	},
	{
		Type:       BloodPressure,
		DeviceType: "bp_monitor",
		Unit:       "mmHg",
		NormalMin:  110,
		NormalMax:  130,
		AlertMin:   90,
		AlertMax:   160,
		Value2Config: &Value2Config{ // Diastolic
			NormalMin: 70,
			NormalMax: 85,
			AlertMin:  60,
			AlertMax:  100,
		},
	},
	{
		Type:       Glucose,
		DeviceType: "glucometer",
		Unit:       "mg/dL",
		NormalMin:  80,
		NormalMax:  140,
		AlertMin:   60,
		AlertMax:   250,
	},
	{
		Type:       Weight,
		DeviceType: "scale",
		Unit:       "kg",
		NormalMin:  60,
		NormalMax:  90,
		AlertMin:   50,
		AlertMax:   120,
	},
	{
		Type:       Steps,
		DeviceType: "watch",
		Unit:       "steps",
		NormalMin:  2000,
		NormalMax:  8000,
		AlertMin:   0,
		AlertMax:   15000,
	},
}

func main() {
	log.Println("Starting eSante Device & Phone Simulator...")

	config := loadConfig()
	
	log.Printf("Configuration:")
	log.Printf("   MQTT Broker: %s:%s", config.MQTTBroker, config.MQTTPort)
	log.Printf("   Number of Patients: %d", config.NumPatients)
	log.Printf("   Emit Interval: %d seconds", config.EmitIntervalSeconds)
	log.Printf("   Alert Interval: every %d seconds", config.AlertIntervalSeconds)

	// Connect to MQTT broker
	client := connectMQTT(config)
	defer client.Disconnect(250)

	log.Println("Connected to MQTT broker")
	log.Println("Starting vitals simulation...")

	// Start simulation loop
	ticker := time.NewTicker(time.Duration(config.EmitIntervalSeconds) * time.Second)
	defer ticker.Stop()

	simulationStartTime := time.Now()
	iterationCount := 0

	for {
		select {
		case <-ticker.C:
			iterationCount++
			shouldGenerateAlert := (iterationCount * config.EmitIntervalSeconds) % config.AlertIntervalSeconds == 0

			if shouldGenerateAlert {
				log.Printf("[ALERT] Cycle %d - generating abnormal values", iterationCount)
			}

			// Emit vitals for all patients
			for patientID := 1; patientID <= config.NumPatients; patientID++ {
				emitVitalsForPatient(client, patientID, shouldGenerateAlert)
			}

			elapsed := time.Since(simulationStartTime)
			log.Printf("[INFO] Iteration %d completed (%d patients) - Running for %v", 
				iterationCount, config.NumPatients, elapsed.Round(time.Second))
		}
	}
}

func loadConfig() Config {
	return Config{
		MQTTBroker:           getEnv("MQTT_BROKER", "mosquitto"),
		MQTTPort:             getEnv("MQTT_PORT", "1883"),
		NumPatients:          getEnvInt("NUM_PATIENTS", 5),
		EmitIntervalSeconds:  getEnvInt("EMIT_INTERVAL_SECONDS", 10),
		AlertIntervalSeconds: getEnvInt("ALERT_INTERVAL_SECONDS", 60),
	}
}

func connectMQTT(config Config) mqtt.Client {
	brokerURL := fmt.Sprintf("tcp://%s:%s", config.MQTTBroker, config.MQTTPort)
	
	opts := mqtt.NewClientOptions()
	opts.AddBroker(brokerURL)
	opts.SetClientID("esante-simulator")
	opts.SetUsername("simulator")
	opts.SetPassword("simulator")
	opts.SetCleanSession(true)
	opts.SetAutoReconnect(true)
	opts.SetConnectRetry(true)
	opts.SetConnectRetryInterval(5 * time.Second)
	
	opts.OnConnect = func(c mqtt.Client) {
		log.Println("[MQTT] Connected")
	}
	
	opts.OnConnectionLost = func(c mqtt.Client, err error) {
		log.Printf("[MQTT] Connection lost: %v", err)
	}

	client := mqtt.NewClient(opts)
	
	// Retry connection
	for i := 0; i < 10; i++ {
		if token := client.Connect(); token.Wait() && token.Error() != nil {
			log.Printf("Failed to connect to MQTT broker (attempt %d/10): %v", i+1, token.Error())
			time.Sleep(3 * time.Second)
		} else {
			return client
		}
	}
	
	log.Fatal("Could not connect to MQTT broker after 10 attempts")
	return nil
}

func emitVitalsForPatient(client mqtt.Client, patientID int, forceAlert bool) {
	for _, vitalConfig := range vitalConfigs {
		payload := generateVitalPayload(patientID, vitalConfig, forceAlert)
		topic := fmt.Sprintf("esante/patient/%d/vitals/%s", patientID, vitalConfig.Type)
		
		jsonData, err := json.Marshal(payload)
		if err != nil {
			log.Printf("Error marshaling payload: %v", err)
			continue
		}

		// Publish with QoS 1 (at least once delivery)
		token := client.Publish(topic, 1, false, jsonData)
		token.Wait()
		
		if token.Error() != nil {
			log.Printf("Error publishing to %s: %v", topic, token.Error())
		}
	}
}

func generateVitalPayload(patientID int, config VitalConfig, forceAlert bool) VitalPayload {
	var value float64
	var value2 *float64

	if forceAlert {
		// Generate alert/abnormal value
		if rand.Float64() < 0.5 {
			value = config.AlertMin - rand.Float64()*5
		} else {
			value = config.AlertMax + rand.Float64()*10
		}

		if config.Value2Config != nil {
			v2 := config.Value2Config.AlertMax + rand.Float64()*5
			value2 = &v2
		}
	} else {
		// Generate normal value
		value = config.NormalMin + rand.Float64()*(config.NormalMax-config.NormalMin)
		
		// Add some realistic variation
		value = addGaussianNoise(value, (config.NormalMax-config.NormalMin)*0.1)
		
		// Ensure within bounds
		value = math.Max(config.NormalMin, math.Min(config.NormalMax, value))

		if config.Value2Config != nil {
			v2 := config.Value2Config.NormalMin + 
				rand.Float64()*(config.Value2Config.NormalMax-config.Value2Config.NormalMin)
			v2 = addGaussianNoise(v2, (config.Value2Config.NormalMax-config.Value2Config.NormalMin)*0.1)
			value2 = &v2
		}
	}

	// Generate realistic metadata with occasional low battery for alerts
	battery := 60 + rand.Intn(40) // 60-100% normally
	if forceAlert && rand.Float64() < 0.3 { // 30% chance of low battery on alert cycles
		battery = 10 + rand.Intn(25) // 10-35% (will trigger <30 alert)
	}
	
	quality := "good"
	if forceAlert || rand.Float64() < 0.1 {
		quality = "poor"
	}

	payload := VitalPayload{
		PatientID:       fmt.Sprintf("patient-%d", patientID),
		DeviceType:      config.DeviceType,
		MeasurementType: config.Type,
		Value:           math.Round(value*100) / 100, // Round to 2 decimals
		Unit:            config.Unit,
		Timestamp:       time.Now().UTC().Format(time.RFC3339),
		Metadata: map[string]interface{}{
			"battery":  battery,
			"quality":  quality,
			"deviceId": fmt.Sprintf("device-%s-%d", config.DeviceType, patientID),
			"firmware": "v1.2.3",
		},
	}

	if value2 != nil {
		rounded := math.Round(*value2*100) / 100
		payload.Value2 = &rounded
	}

	return payload
}

func addGaussianNoise(value, stddev float64) float64 {
	// Box-Muller transform for Gaussian noise
	u1 := rand.Float64()
	u2 := rand.Float64()
	noise := math.Sqrt(-2*math.Log(u1)) * math.Cos(2*math.Pi*u2) * stddev
	return value + noise
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func getEnvInt(key string, defaultValue int) int {
	if value := os.Getenv(key); value != "" {
		if intVal, err := strconv.Atoi(value); err == nil {
			return intVal
		}
	}
	return defaultValue
}
