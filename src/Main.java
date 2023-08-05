package src;

//import static org.junit.Assert.*;
//import static org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.openqa.selenium.By;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

//Holds information for making a bet
class BetLog{
    String betcolor;
    String betamount;

    BetLog()
    {
        this.betcolor = "Blue";
        this.betamount = "100";
    }
    BetLog(String bc)
    {
        this.betcolor = bc;
        this.betamount = "100";
    }
}


public class Main {
    //Tracks how many bets have been made since starting the program
    static int BetCount = 0;

    //For establishing connections between chrom and the database
    static Connection conn = null;
    static WebDriver wdriver;

    public static void main(String[] arg) {
        //Find database
        connect();

        // Setup Chrome driver settings
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        // Begin instance of Chrome
        wdriver = new ChromeDriver(options);

        // Maximize window
        wdriver.manage().window().maximize();
        // Go to desired website (Salty Bet)
        wdriver.get("https://www.saltybet.com/");

        // Find and click the Sign In button
        WebElement ButtonSignIn = wdriver.findElement(By.xpath("//span[text()='Sign in']"));
        ButtonSignIn.click();

        String Username = "";
        String Password = "";
        try 
        {
            File myObj = new File("credentials.txt");
            Scanner myReader = new Scanner(myObj);
            Username = myReader.nextLine();
            Password = myReader.nextLine();
            myReader.close();
        } 
        catch (FileNotFoundException e) 
        {
            throw new RuntimeException("Failed to find credentials file");
        }

        // Find the Email entry box and input email
        WebElement TextBoxEmail = wdriver.findElement(By.id("email"));
        TextBoxEmail.sendKeys(Username);

        // Find the Password entry box and input email
        WebElement TextBoxPword = wdriver.findElement(By.id("pword"));
        TextBoxPword.sendKeys(Password);

        // Find and click the Sign In / Submit button
        WebElement ButtonSubmit = wdriver.findElement(By.className("submit"));
        ButtonSubmit.click();

        // Find the BetStatus text
        WebElement TextBetStatus = wdriver.findElement(By.id("betstatus"));

        // Wait until a fresh betting
        while (!TextBetStatus.getText().equals("Bets are OPEN!")) 
        {

        }
        // Capture betting elements
        WebElement TextBoxWager = wdriver.findElement(By.id("wager"));
        WebElement ButtonRedBetSelect = wdriver.findElement(By.id("player1"));
        WebElement ButtonBlueBetSelect = wdriver.findElement(By.id("player2"));
        // Begin loop program
        Run(TextBetStatus, TextBoxWager, ButtonRedBetSelect, ButtonBlueBetSelect);

    }

    static void Run(WebElement betstatus, WebElement wagerbox, WebElement betredbutton, WebElement betbluebutton) 
    {
        //Run program until user tells it to stop
        while (true) 
        {
            //Get references to the buttons for betting on the red or blue fighters
            WebElement rf = wdriver.findElement(By.xpath("//input[@name='player1']"));
            WebElement bf = wdriver.findElement(By.xpath("//input[@name='player2']"));
            //Get bet info
            BetLog BL = BetLogic(rf.getAttribute("value"), bf.getAttribute("value"));

            //Input the bet based on the received information
            wagerbox.sendKeys(BL.betamount);
            switch(BL.betcolor)
            {
                case("Red"):
                    betredbutton.click();
                    break;
                case("Blue"):
                    betbluebutton.click();
                    break;
            }
            //Increment the number of fights the program has been on for
            ++BetCount;

            System.out.println(BetCount);

            // Wait for match to start
            while (!betstatus.getText().equals("Bets are locked until the next match.")) // Not condition accounts for empty transition
            {

            }

            // Wait for match to play out
            while (!betstatus.getText().contains("Payouts to Team")) // Not condition accounts for empty transition
            {

            }

            // CAPTURE RESULTS
            String results = betstatus.getText();

            WebElement AllFighter = wdriver.findElement(By.xpath("//span[@id='odds']"));
            String allfighter = AllFighter.getText();  

            //Find the seperating dollar sign
            int rindex = allfighter.indexOf("$");
            //Grab the red fighter out of the string
            String redfighter = allfighter.substring(0, rindex - 1).trim();
                                                        //Cut out the dollar sign
            String minusredfighter = allfighter.substring(rindex + 1);

            //Find the seperating space
            int rbindex = minusredfighter.indexOf(" ");
            //Grab the red bets
            String redbets = minusredfighter.substring(0, rbindex);
                                                        //Cut out the space
            String minusred = minusredfighter.substring(rbindex + 1);

            //Find the seperating dollar sign
            int bindex = minusred.indexOf("$");
            //Grab the blue fighter out of the string
            String bluefighter = minusred.substring(0, bindex - 1).trim();
            //Just leaves the blue bets         //Cut out the dollar sign
            String bluebets = minusred.substring(bindex + 1);

            //Remove all commas and make an int
            int totalredbet = Integer.parseInt(redbets.replaceAll(",", ""));

            //Remove all commas and make an int
            int totalbluebet = Integer.parseInt(bluebets.replaceAll(",", ""));
            
            //Figure out who won the match
            String winner;
            if (results.contains("Red")) 
            {
                winner = "Red";
            } 
            else 
            {
                winner = "Blue";
            }

            //SQL Passing, adding to database
            insert(BL.betcolor, Integer.parseInt(BL.betamount), winner, totalredbet, totalbluebet, redfighter, bluefighter);

            // Wait for next betting
            while (!betstatus.getText().equals("Bets are OPEN!")) // Not condition accounts for empty transition
            {

            }
        }
    }

    public static BetLog BetLogic(String redfighter, String bluefighter)
    {
        System.out.println("red player " + redfighter);
        System.out.println("blue player " + bluefighter);
        //Exclude generic team names that wont follow data patterns
        if (!redfighter.equals("Team A") && !redfighter.equals("Team B") && !bluefighter.equals("Team A") && !bluefighter.equals("Team B"))
        {
            //Are the fighters in the database
            Integer fightCountr = 0;
            //Pull all the fights for the first fighter from the database
            String sqlredfighter = """
            SELECT COUNT(*) FROM Bets WHERE RedFighter = ? OR BlueFighter = ? """;
            try
            {
                PreparedStatement stmtr = conn.prepareStatement(sqlredfighter);
                stmtr.setString(1, redfighter);
                stmtr.setString(2, redfighter);

                ResultSet rsr = stmtr.executeQuery();
                fightCountr = rsr.getInt(1); 
            }
            catch (SQLException e) 
            {
                System.out.println(e.getMessage());
            }
            System.out.println("Red Fought " + fightCountr);
            //If the first fighter is in the database
            if(fightCountr > 0)
            {
                Integer fightCountb = 0;
                //Pull all the fights for the second fighter
                String sqlbluefighter = """
                    SELECT COUNT(*) FROM Bets WHERE RedFighter = ? OR BlueFighter = ? """;
                try
                {
                    PreparedStatement stmtb = conn.prepareStatement(sqlbluefighter);
                    stmtb.setString(1, bluefighter);
                    stmtb.setString(2, bluefighter);

                    ResultSet rsrb = stmtb.executeQuery();
                    fightCountb = rsrb.getInt(1);
                }
                catch (SQLException e)
                {
                    System.out.println(e.getMessage());
                }
                System.out.println("Blue Fought " + fightCountb);
                //If the second fighter is in the database
                if(fightCountb > 0)
                {
                    //Have the fighters fought before
                    Integer rwon = 0;
                    Integer bwon = 0;
                    //Get all the fights where the fighters fought each other
                    String prevFights = """
                            SELECT * FROM Bets WHERE RedFighter = ? AND BlueFighter = ? OR RedFighter = ? AND BlueFighter = ?""";
                    try
                    {
                        PreparedStatement stmt = conn.prepareStatement(prevFights);
                        stmt.setString(1, redfighter);
                        stmt.setString(2, bluefighter);
                        stmt.setString(3, bluefighter);
                        stmt.setString(4, redfighter);

                        ResultSet rs = stmt.executeQuery();
                        //Loop through the fights and see who has won more
                        while(rs.next())
                        {
                            //Match the wincolor to the fighter to make a win
                            String winner = rs.getString("WinColor");
                            if(winner.equals("Red"))
                            {
                                String redF = rs.getString("RedFighter");
                                if(redF.equals(redfighter))
                                {
                                    ++rwon;
                                }
                                else
                                    ++bwon;
                            }
                            else
                            {
                                String blueF = rs.getString("BlueFighter");
                                if(blueF.equals(redfighter))
                                {
                                    ++rwon;
                                }
                                else
                                    ++bwon;
                            }
                        }
                        rs.close();
                        System.out.println("RED beat blue " + rwon);
                        System.out.println("BLUE beat red " + bwon);
                    }
                    catch (SQLException e) 
                    {
                        System.out.println(e.getMessage());
                    }
                    //If the red fighter has beaten the blue fighter more
                    if(rwon > bwon)
                    {
                        //Bet on red
                        BetLog log = new BetLog("Red");
                            log.betamount = "1000";

                        return log;
                    }
                    //If the blue fighter has beaten the red fighter more
                    else if(bwon > rwon)
                    {
                        //Bet on blue (default color)
                        BetLog log = new BetLog();
                        log.betamount = "1000";

                        return log;
                    }

                    //If the fighters haven't fought each other before, What are the fighter win ratios
                    //Fighter 1 (redfighter)
                    float redWL = 0.0f;
                    //Compare the fighters wins to total fights and return as a percent
                    String sqlfred = """
                        SELECT ROUND(CAST(COUNT(*) AS float) / (SELECT CAST(COUNT(*) AS float) FROM Bets WHERE RedFighter = ? OR  BlueFighter = ?), 2)
                                AS WinLoss FROM Bets WHERE RedFighter = ? AND WinColor = 'Red' OR BlueFighter = ? AND WinColor = 'Blue';""";
                    try
                    {
                        PreparedStatement stmt = conn.prepareStatement(sqlfred);
                        stmt.setString(1, redfighter);
                        stmt.setString(2, redfighter);
                        stmt.setString(3, redfighter);
                        stmt.setString(4, redfighter);

                        ResultSet rsr = stmt.executeQuery();
                        redWL = rsr.getFloat(1);
                    }
                    catch (SQLException e) 
                    {
                        System.out.println(e.getMessage());
                    }
                
                                
                    //Fighter 2 (bluefighter)
                    float blueWL = 0.0f;
                    String sqlfblue = """
                        SELECT ROUND(CAST(COUNT(*) AS float) / (SELECT CAST(COUNT(*) AS float) FROM Bets WHERE RedFighter = ? OR  BlueFighter = ?), 2)
                                AS WinLoss FROM Bets WHERE RedFighter = ? AND WinColor = 'Red' OR BlueFighter = ? AND WinColor = 'Blue';""";

                    try
                    {
                        PreparedStatement stmt = conn.prepareStatement(sqlfblue);
                        stmt.setString(1, bluefighter);
                        stmt.setString(2, bluefighter);
                        stmt.setString(3, bluefighter);
                        stmt.setString(4, bluefighter);

                        ResultSet rsb = stmt.executeQuery();
                        blueWL = rsb.getFloat(1);
                    }
                    catch (SQLException e) 
                    {
                        System.out.println(e.getMessage());
                    }
                    
                    System.out.println("Red percent " + redWL);
                    System.out.println("Blue percent " + blueWL);
                    //If red has a higher win percent
                    if(redWL > blueWL)
                    {
                        //Bet on red
                        BetLog log = new BetLog("Red");
                        //If win rate is much better bet more
                        if(redWL > blueWL * 2)
                            log.betamount = "400";
                        else
                            log.betamount = "200";

                        return log;
                        
                    }
                    //If blue has better win rate
                    if(redWL < blueWL)
                    {
                        //Bet on blue (default)
                        BetLog log = new BetLog();
                        if(redWL * 2 < blueWL)
                            log.betamount = "400";
                        else
                            log.betamount = "200";

                        return log;
                    }
                }
            }
        }
        //If there isn't enough info return default bet
        return new BetLog();
    }

    public static void connect() 
    {
        String Filepath = "";
        try 
        {
            File myObj = new File("dbloc.txt");
            Scanner myReader = new Scanner(myObj);
            Filepath = myReader.nextLine();
            myReader.close();
        } 
        catch (FileNotFoundException e) 
        {
            throw new RuntimeException("Failed to find database file");
        }
        
        try 
        {
            
            // db parameters
            String url = Filepath;
            // create a connection to the database
            conn = DriverManager.getConnection(url);

            System.out.println("Connection to SQLite has been established.");

        } 
        catch (SQLException e) 
        {
            System.out.println(e.getMessage());
        } 
        
    }

    public static void insert(String betcolor, int amountbet, String wincolor, int redbets, int bluebets, String redfighter, String bluefighter) 
    {
        //Setup formt and command to insert data into SQL database
        String sql = """
            INSERT INTO Bets (BetColor, AmountBet, WinColor, RedBets, BlueBets, RedFighter, BlueFighter) 
            VALUES (?, ?, ?, ?, ?, ?, ?)""";

        try  
        {
            //Provide information to SQL command for adding data
            PreparedStatement prep = conn.prepareStatement(sql);
            prep.setString(1, betcolor);
            prep.setInt(2, amountbet);
            prep.setString(3, wincolor);
            prep.setInt(4, redbets);
            prep.setInt(5, bluebets);
            prep.setString(6, redfighter);
            prep.setString(7, bluefighter);
            //Make update
            prep.executeUpdate();
        } 
        catch (SQLException e) 
        {
            System.out.println(e.getMessage());
        }
    }
}
