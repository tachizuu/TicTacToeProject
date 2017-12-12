var url = "ws://localhost:8080/TicTacToeServer/tttserver";
var ws = new WebSocket(url);

function sendMessage(text)
{
    ws.send(text);
}

ws.onmessage = function processMessage(message)
{
    var jsonData = JSON.parse(message.data);
    switch(jsonData.type)
    {
        case "boxClick":
        {
            var color = colorCalc(jsonData.color);
            document.getElementById(jsonData.box).style.backgroundColor = color;
        }
        break;
        case "getAll":
        {
            for (var i = 0; i < jsonData.box.length; i++)
            {
                var box = jsonData.box[i].name;
                var color = colorCalc(jsonData.box[i].color);
                document.getElementById(box).style.backgroundColor = color;
            }
        }
        break;
        case "gameReset":
        {
            for(var i = 1; i <= 9; i++)
            {
                document.getElementById("gamebox" + i).style.backgroundColor = "white";
            }
        }
        break;
        case "winner":
        {
            alert(jsonData.username + " won!");
        }
        break;
        case "queue":
        {
            var ul = document.getElementById("userlist");
            ul.innerHTML = "";
            for(var i = 0; i < jsonData.user.length; i++)
            {
                var name = jsonData.user[i].username;
                var li = "<li>" + (i + 1) + ": " + name + "</li>";
                ul.innerHTML += li;
            }
        }
        break;
    }
};

window.onload = function()
{
    //ge alla gameboxar onclick
    var username = prompt("Username?", "");
    if(username === null || username === undefined || username === '')
    {
        username = "Anonymous";
    }
    sendMessage("Username:" + username);
    
    for(var i = 1; i <= 9; i++)
    {
        document.getElementById("gamebox" + i).addEventListener("click", function()
        {
            sendMessage(this.id);
        });
    }
};

function colorCalc(colorCode)
{
    switch(colorCode)
    {
        case 1:
            return "lightgreen";
            break;
        case -1:
            return "lightblue";
            break;
        default:
            return "white";
    }
}/**/