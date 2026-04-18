var stompClient = null;

function setConnected(connected) {
    if (connected) {
        document.getElementById("connect").setAttribute("disabled", 'true');
        document.getElementById("connect").classList.add("disabled");
    } else {
        
        document.getElementById("connect").removeAttribute("disabled");
        document.getElementById("connect").classList.remove("disabled");
        
        
    }
    document.getElementById("messages").innerHTML = "";
}

function connect() {
    // TODO: Implement connect function
    var socket = new SockJS('/my-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/messages', function (message) {
            showMessage(JSON.parse(message.body).content);
        });
    });
}

function showMessage(message) {
    const messagesDiv = document.getElementById("messages");
    messagesDiv.innerHTML += `<div>${message}</div>`; // Append new message

}

window.addEventListener('load',
    function () {
        document.getElementById("connect").addEventListener('click', (e) => {
            e.preventDefault();
            connect();
        });

    }, false);