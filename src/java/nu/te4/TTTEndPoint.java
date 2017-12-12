package nu.te4;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/tttserver")
public class TTTEndPoint
{
    //static Set<Session> sessions = new HashSet<>();
    static ArrayList<Session> sessions = new ArrayList<>();
    static ArrayList<Box> boxList = new ArrayList<>();
    static int boxValues[] = {2, 7, 6, 9, 5, 1, 4, 3, 8};
    private static int colorTracker = -1;
    private static int turn = 0;
    
    @OnMessage
    public void onMessage(String message, Session user) throws IOException
    {
        if(user.getUserProperties().get("username") == null)   //användare har inget användarnamn
        {
            System.out.println("no username");
            try
            {
                String username = message.substring(message.indexOf(":") + 1);
                user.getUserProperties().put("username", username);
                
                //uppdatera kö-lista
                updateQueue();
            }
            catch(Exception e)
            {}
            
        }
        else if(sessions.indexOf(user) == turn)  //kollar så att klicket kommer från rätt spelare
        {  
            
            Box b = new Box(message);
            if(boxList.size() >= 9)
            {
                //planen är full
                resetGame();
            }
            else if(boxList.contains(b))
            {
                //boxen har redan klickats på
                System.out.println("Box has been clicked");
            }
            else
            {
                //byter tur
                turn++;
                if(turn > 1){turn = 0;}
                
                boxList.add(b);
                b.setColor(getColor());
            
                String resp = Json.createObjectBuilder()
                        .add("type", "boxClick")
                        .add("box", b.getName())
                        .add("color", b.getColor())
                        .build().toString();
            
                for (Session session : sessions)
                {
                    session.getBasicRemote().sendText(resp);
                }
                int winner = winCheck();    //förlorare = (winner * -1) + 1
                if(winner > -1)             //ex:   (1 * -1) + 1 = 0
                {                           //      (0 * -1) + 1 = 1
                    String winnerName = (String)sessions.get(winner).getUserProperties().get("username");
                    
                    //skicka data om vinnare till klienter
                    resp = Json.createObjectBuilder()
                            .add("type", "winner")
                            .add("username", winnerName)
                            .build().toString();
                    for (Session session : sessions)
                    {
                        session.getBasicRemote().sendText(resp);
                    }
                    
                    //flyttar förloraren sist i kön
                    int loser = (winner * -1) + 1;
                    listMoveToLast(sessions.get(loser));
                    
                    //startar om spelet
                    resetGame();
                }
            }
        }
        else
        {
            System.out.println(sessions.indexOf(user) + " - not your turn!");
        }
    }
    
    @OnOpen
    public void open(Session user) throws IOException
    {
        sessions.add(user);
        System.out.println(sessions.indexOf(user) + ": " + user);
        
        //skicka spel-info
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("type", "getAll");
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for(Box box:boxList)
        {
            jsonArrayBuilder.add(Json.createObjectBuilder()
            .add("name", box.getName())
            .add("color", box.getColor()).build());
        }
        jsonObjectBuilder.add("box", jsonArrayBuilder.build());
        String resp = jsonObjectBuilder.build().toString();
        user.getBasicRemote().sendText(resp);
    }
    
    @OnClose
    public void close(Session user) throws IOException
    {
        sessions.remove(user);
        System.out.println("disconnected: " + user);
        if(sessions.isEmpty())
        {
            resetGame();
        }
    }
    
    private void listMoveToLast(Session target)
    {
        int index = sessions.indexOf(target);
        sessions.add(sessions.get(index));
        sessions.remove(index);
    }
    
    private int getColor()
    {
        colorTracker *= -1;
        return colorTracker;
    }
    
    private void resetGame() throws IOException
    {
        boxList.clear();
        colorTracker = -1;
        updateQueue();
        turn = 0;
        //uppdatera klienter
        for(Session user:sessions)
        {
            String msg = Json.createObjectBuilder()
                    .add("type", "gameReset")
                    .build().toString();
            user.getBasicRemote().sendText(msg);
        }
        System.out.println("Game reset");
    }
    
    public int winCheck()
    {
        //horisontellt
        for(int i = 1; i <= 7; i += 3)  //för varje rad
        {
            int rowSum = 0;
            
            for(int j = 0; j < 3; j++)  //för varje box
            {
                Box compareBox = new Box("gamebox" + (i + j));
                if(boxList.contains(compareBox))
                {
                    int color = boxList.get(boxList.indexOf(compareBox)).getColor();
                    rowSum += boxValues[(compareBox.getId() - 1)] * color;
                }
            }
            float winner = (float)rowSum / 15;
            if(winner == 1)   //om winner är exakt -1 eller 1 så har någon vunnit
            {
                return 0;   //returnerar spelare 1s index i session-listan
            }
            else if(winner == -1)
            {
                return 1;   //spelare 2s index
            }
        }
        
        //vertikalt
        for(int i = 1; i <= 3; i++)
        {
            int colSum = 0;
            
            for(int j = 0; j <= 6; j += 3)
            {
                Box compareBox = new Box("gamebox" + (i + j));
                if(boxList.contains(compareBox))
                {
                    int color = boxList.get(boxList.indexOf(compareBox)).getColor();
                    colSum += boxValues[(compareBox.getId() - 1)] * color;
                }
            }
            float winner = (float)colSum / 15;
            if(winner == 1)   //om winner är exakt -1 eller 1 så har någon vunnit
            {
                return 0;   //returnerar spelare 1s index i session-listan
            }
            else if(winner == -1)
            {
                return 1;   //spelare 2s index
            }
        }
        
        //ingen har vunnit, returnera 0
        return -1;
    }
    
    public void updateQueue() throws IOException
    {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        jsonObjectBuilder.add("type", "queue");
        for(Session session : sessions)
        {
            try
            {
                String username = (String)session.getUserProperties().get("username");
                jsonArrayBuilder.add(Json.createObjectBuilder()
                    .add("username", username)
                    .build());
            }
            catch(Exception e)
            {}
        }
        jsonObjectBuilder.add("user", jsonArrayBuilder.build());
        String resp = jsonObjectBuilder.build().toString();
        System.out.println(resp);
        for(Session session : sessions)
        {
            session.getBasicRemote().sendText(resp);
        }
    }
}
