# autobet-saltybet
The product of this project is a program that will collect data from Salty Bet and predict
outcomes of future fights. I designed this using Selenium, SQL, and Java.
Visual Studio Code is the IDE I used and SQLite is my database software.

Using the Java libraries for Selenium this program connects to and logs in to Salty Bet.
After each arcade fight it collects and stores the fighter and results in a SQL database for
future predictions. Then for future fights, using a combination of lookup statements and 
sub queries, it determines which fighter has a higher win rate or if a fighter has beaten
the other before and bets accordingly.

I didn't put a large emphasis on the predictive algorithm as using the tools was the focus
of this project. Bearing that in mind I found this to be a successful project where the data 
was organized and stored by the appropriate tools working together. It also successfully
made varying bets that reflected the data set that had been gathered up to the present point.