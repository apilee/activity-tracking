package main

import (
	"bytes"
	"fmt"
	"github.com/garyburd/redigo/redis"
	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
	"net/http"
	"time"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

type messageData struct {
	msgType int
	msg     []byte
	err     error
}

func main() {
	getActivity := initDatabaseAccessFunction()

	m := mux.NewRouter()

	m.HandleFunc("/{user}", func(w http.ResponseWriter, r *http.Request) {
		params := mux.Vars(r)
		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			fmt.Println("Could not upgrade connection:", err)
			return
		}

		msgChan := make(chan messageData)
		continueChan := make(chan bool)
		go func() {
			for (<-continueChan) == true {
				msgType, msg, err := conn.ReadMessage()
				myMessageData := messageData{msgType, msg, err}
				msgChan <- myMessageData
			}
		}()

		timeout := time.NewTimer(time.Minute * 5)
		sendTicker := time.NewTicker(time.Minute * 1)

		for {
			select {
			case <-sendTicker.C:
				activity, err := getActivity(params["user"])
				if err != nil {
					fmt.Println("Something went wrong: %s", err)
					conn.WriteMessage(websocket.TextMessage, []byte("Error"))
					break
				}

				err = conn.WriteMessage(websocket.TextMessage, []byte(activity))
				if err != nil {
					continueChan <- false
					fmt.Println(err)
					conn.Close()
					return
				}
				continueChan <- true

			case curMessageData := <-msgChan:
				if err != nil {
					fmt.Println("Error when reading:", err)
					conn.Close()
					return
				}
				if bytes.Equal([]byte(curMessageData.msg), []byte("keepalive")) {
					timeout.Reset(5 * time.Minute)
				}

			case <-timeout.C:
				conn.Close()
				return
			}
		}
	})

	fmt.Println("Starting listen server.")

	http.ListenAndServe(":3001", m)
}

func initDatabaseAccessFunction() func(user string) (string, error) {
	c, err := redis.Dial("tcp", "redis:6379")
	for err != nil {
		fmt.Println("Error when connecting to the Redis DB. Trying again after 2 seconds. Error: %s", err)
		err = nil
		c, err = redis.Dial("tcp", "redis:6379")
		time.Sleep(time.Second * 2)
	}

	return func(user string) (string, error) {
		return redis.String(c.Do("GET", user))
	}
}
