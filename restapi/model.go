package restapi

type acceleration struct {
	X float64 `json:"x"`
	Y float64 `json:"y"`
	Z float64 `json:"z"`
}
type orientation struct {
	Azimuth float64 `json:"azimuth"`
	Pitch   float64 `json:"pitch"`
	Roll    float64 `json:"roll"`
}
type gyro struct {
	Roll  float64 `json:"roll"`
	Pitch float64 `json:"pitch"`
	Yaw   float64 `json:"yaw"`
}

// Metadata for training acceleration.
type AccelOrientTraining struct {
	UserId       string       `json:"userID"`
	Timestamp    int64        `json:"timestamp"`
	Activity     string       `json:"activity"`
	StartTime    int64        `json:"starttime"`
	Acceleration acceleration `json:"acceleration"`
	Orientation  orientation  `json:"orientation"`
	Matrix       []float64    `json:"rotmatrix"`
}

// Metadata for production acceleration.
type AccelOrientProduction struct {
	UserId       string       `json:"userID"`
	Timestamp    int64        `json:"timestamp"`
	Acceleration acceleration `json:"acceleration"`
	Orientation  orientation  `json:"orientation"`
	Matrix       []float64    `json:"rotmatrix"`
}

type GyroTraining struct {
	UserId    string `json:"userID"`
	Timestamp int64  `json:"timestamp"`
	Activity  string `json:"activity"`
	StartTime int64  `json:"starttime"`
	Gyro      gyro   `json:"gyro"`
}

type GyroProduction struct {
	UserId    string `json:"userID"`
	Timestamp int64  `json:"timestamp"`
	Gyro      gyro   `json:"gyro"`
}
