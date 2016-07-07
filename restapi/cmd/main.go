package main

import (
	"encoding/json"
	"fmt"
	"github.com/apilee/activity-tracking/restapi"
	"github.com/gocql/gocql"
	"github.com/gorilla/mux"
	"io/ioutil"
	"net"
	"net/http"
	"os"
	"time"
)

var session *gocql.Session

func main() {
	var err error
	add := make([]string, 0, 5)

	add, err = net.LookupHost("cassandra")
	if err != nil {
		fmt.Println(err)
		return
	}
	credentials := gocql.PasswordAuthenticator{Username: os.Getenv("CASSANDRA_USERNAME"), Password: os.Getenv("CASSANDRA_PASSWORD")}
	cluster := gocql.NewCluster(add[0])
	if len(credentials.Username) > 0 {
		cluster.Authenticator = credentials
	}
	cluster.Timeout = time.Second * 4
	cluster.ProtoVersion = 4
	session, err = cluster.CreateSession()
	for err != nil {
		fmt.Println("Error when connecting for keyspace creation. Trying again in 2 seconds.")
		fmt.Println(err)
		err = nil
		session, err = cluster.CreateSession()
		time.Sleep(time.Second * 2)
	}

	err = initKeyspace()
	if err != nil {
		fmt.Println(fmt.Println("Error when creating keyspace:"))
		fmt.Println(err)
		return
	}

	session.Close()

	cluster = gocql.NewCluster(add[0])
	if len(credentials.Username) > 0 {
		cluster.Authenticator = credentials
	}
	cluster.Timeout = time.Second * 4
	cluster.ProtoVersion = 4
	cluster.Keyspace = "activitytracking"
	session, err = cluster.CreateSession()
	for err != nil {
		fmt.Println("Error when connecting for active use. Trying again in 2 seconds.")
		fmt.Println(err)
		err = nil
		session, err = cluster.CreateSession()
		time.Sleep(time.Second * 2)
	}

	// Create tables if non-existent.
	err = initAccelerationProductionTable()
	if err != nil {
		fmt.Println(err)
		return
	}
	err = initAccelerationTrainingTable()
	if err != nil {
		fmt.Println(err)
		return
	}
	err = initGyroProductionTable()
	if err != nil {
		fmt.Println(err)
		return
	}
	err = initGyroTrainingTable()
	if err != nil {
		fmt.Println(err)
		return
	}
	err = initOrientationProductionTable()
	if err != nil {
		fmt.Println(err)
		return
	}
	err = initOrientationTrainingTable()
	if err != nil {
		fmt.Println(err)
		return
	}

	fmt.Println("Initialization complete.")

	m := mux.NewRouter()
	m.HandleFunc("/production/accelorient", handleAccelOrientProduction)
	m.HandleFunc("/training/accelorient", handleAccelOrientTraining)
	m.HandleFunc("/production/gyro", handleGyroProduction)
	m.HandleFunc("/training/gyro", handleGyroTraining)
	m.HandleFunc("/healthcheck", handleHealthcheck)
	http.ListenAndServe(":3000", m)
}

func initKeyspace() error {
	err := session.Query(`CREATE KEYSPACE IF NOT EXISTS activitytracking WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };`).Exec()
	if err != nil {
		return err
	}
	return nil
}

func initAccelerationTrainingTable() error {
	// Create the Cassandra table if not there already.
	err := session.Query(`CREATE TABLE IF NOT EXISTS trainingAcceleration (userid text, activity text, starttime timestamp, time timestamp, x double, y double, z double, PRIMARY KEY (userid, starttime, time));`).Exec()
	if err != nil {
		return err
	}
	return nil
}

func initAccelerationProductionTable() error {
	// Create the Cassandra table if not there already.
	err := session.Query(`CREATE TABLE IF NOT EXISTS productionAcceleration (userid text, time timestamp, x double, y double, z double, PRIMARY KEY (userid, time));`).Exec()
	if err != nil {
		return err
	}
	return nil
}

func initGyroTrainingTable() error {
	// Create the Cassandra table if not there already.
	err := session.Query(`CREATE TABLE IF NOT EXISTS trainingGyro (userid text, activity text, starttime timestamp, time timestamp, pitch double, roll double, yaw double, PRIMARY KEY (userid, starttime, time));`).Exec()
	if err != nil {
		return err
	}
	return nil
}

func initGyroProductionTable() error {
	// Create the Cassandra table if not there already.
	err := session.Query(`CREATE TABLE IF NOT EXISTS productionGyro (userid text, time timestamp, pitch double, roll double, yaw double, PRIMARY KEY (userid, time));`).Exec()
	if err != nil {
		return err
	}
	return nil
}

func initOrientationTrainingTable() error {
	// Create the Cassandra table if not there already.
	err := session.Query(`CREATE TABLE IF NOT EXISTS trainingOrientation (userid text, activity text, starttime timestamp, time timestamp, azimuth double, pitch double, roll double, PRIMARY KEY (userid, starttime, time));`).Exec()
	if err != nil {
		return err
	}
	return nil
}

func initOrientationProductionTable() error {
	// Create the Cassandra table if not there already.
	err := session.Query(`CREATE TABLE IF NOT EXISTS productionOrientation (userid text, time timestamp, azimuth double, pitch double, roll double, PRIMARY KEY (userid, time));`).Exec()
	if err != nil {
		return err
	}
	return nil
}

func handleAccelOrientProduction(w http.ResponseWriter, r *http.Request) {
	// Read and parse request data.
	myData := &restapi.AccelOrientProduction{}
	data, err := ioutil.ReadAll(r.Body)
	if err != nil {
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	err = json.Unmarshal(data, &myData)
	if err != nil {
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Insert data into Cassandra.
	err = session.Query(`INSERT INTO productionAcceleration (userid, timestamp, x, y, z) VALUES (?, ?, ?, ?, ?, ?)`,
		myData.UserId,
		myData.Timestamp,
		myData.Acceleration.X,
		myData.Acceleration.Y,
		myData.Acceleration.Z,
	).Exec()
	if err != nil {
		fmt.Println("Error when inserting acceleration:")
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	err = session.Query(`INSERT INTO productionOrientation (userid, timestamp, azimuth, pitch, roll) VALUES (?, ?, ?, ?, ?, ?)`,
		myData.UserId,
		myData.Timestamp,
		myData.Orientation.Azimuth,
		myData.Orientation.Pitch,
		myData.Orientation.Roll,
	).Exec()
	if err != nil {
		fmt.Println("Error when inserting orientation:")
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Android app expects the Status Created code for responses signaling success.
	w.WriteHeader(http.StatusCreated)
}

func handleAccelOrientTraining(w http.ResponseWriter, r *http.Request) {
	// Read and parse request data.
	myData := &restapi.AccelOrientTraining{}
	data, err := ioutil.ReadAll(r.Body)
	if err != nil {
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	err = json.Unmarshal(data, &myData)
	if err != nil {
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Insert data into Cassandra.
	err = session.Query(`INSERT INTO trainingAcceleration (userid, activity, starttime, time, x, y, z) VALUES (?, ?, ?, ?, ?, ?, ?);`,
		myData.UserId,
		myData.Activity,
		myData.StartTime,
		myData.Timestamp,
		myData.Acceleration.X,
		myData.Acceleration.Y,
		myData.Acceleration.Z,
	).Exec()
	if err != nil {
		fmt.Println("Error when inserting acceleration:")
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	err = session.Query(`INSERT INTO trainingOrientation (userid, activity, starttime, time, azimuth, pitch, roll) VALUES (?, ?, ?, ?, ?, ?, ?);`,
		myData.UserId,
		myData.Activity,
		myData.StartTime,
		myData.Timestamp,
		myData.Orientation.Azimuth,
		myData.Orientation.Pitch,
		myData.Orientation.Roll,
	).Exec()
	if err != nil {
		fmt.Println("Error when inserting orientation:")
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Android app expects the Status Created code for responses signaling success.
	w.WriteHeader(http.StatusCreated)
}

func handleGyroProduction(w http.ResponseWriter, r *http.Request) {
	// Read and parse request data.
	myData := &restapi.GyroProduction{}
	data, err := ioutil.ReadAll(r.Body)
	if err != nil {
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	err = json.Unmarshal(data, &myData)
	if err != nil {
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Insert data into Cassandra.
	err = session.Query(`INSERT INTO productionGyro (userid, time, pitch, roll, yaw) VALUES (?, ?, ?, ?, ?, ?, ?);`,
		myData.UserId,
		myData.Timestamp,
		myData.Gyro.Pitch,
		myData.Gyro.Roll,
		myData.Gyro.Yaw,
	).Exec()
	if err != nil {
		fmt.Println("Error when inserting:")
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	// Android app expects the Status Created code for responses signaling success.
	w.WriteHeader(http.StatusCreated)
}

func handleGyroTraining(w http.ResponseWriter, r *http.Request) {
	// Read and parse request data.
	myData := &restapi.GyroTraining{}
	data, err := ioutil.ReadAll(r.Body)
	if err != nil {
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	err = json.Unmarshal(data, &myData)
	if err != nil {
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Insert data into Cassandra.
	err = session.Query(`INSERT INTO trainingGyro (userid, activity, starttime, time, pitch, roll, yaw) VALUES (?, ?, ?, ?, ?, ?, ?);`,
		myData.UserId,
		myData.Activity,
		myData.StartTime,
		myData.Timestamp,
		myData.Gyro.Pitch,
		myData.Gyro.Roll,
		myData.Gyro.Yaw,
	).Exec()
	if err != nil {
		fmt.Println("Error when inserting:")
		fmt.Println(err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	// Android app expects the Status Created code for responses signaling success.
	w.WriteHeader(http.StatusCreated)
}

func handleHealthcheck(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
}
