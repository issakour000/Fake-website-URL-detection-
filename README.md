# Fake-website-URL-detection-
A desktop application, written in pure Core Java, that checks a URL and tells you whether it looks Safe, Suspicious, or Phishing. It combines a rule-based checklist with a Logistic Regression model built from scratch.

It uses no external ML libraries (TensorFlow, Weka) and no database server (MySQL, etc.). Data is stored in plain .txt / .tsv files for simplicity. But it can be used.

# Features
URL Scanner: check a single link and get a score and verdict
Batch Processor: scan a whole list of URLs at once
History and Statistics: your past scans in a table (newest first) plus a pie chart of Safe / Suspicious / Phishing percentages
ML Model Trainer: load a dataset, train the model, view Accuracy, F1 score and Confusion Matrix, and test predictions manually
Simulation Panel: experiment with URLs
User Accounts: register and log in with salted and hashed passwords
Responsive UI: slow tasks run in background SwingWorker threads so the window never freezes
Custom "Bento" design: rounded cards and flat colors built with Swing and AWT

# Tech Stack
Area	Technology
Language	Core Java
UI	Java Swing and AWT (custom "Bento" design)
Storage	Flat files (.txt, .tsv) with synchronized I/O
Machine Learning	Custom Logistic Regression with Gradient Descent
Concurrency	SwingWorker background threads


                    +-------------+
                    |  Main.java  |
                    +------+------+
                           |
                    +------v------+
                    | LoginFrame  |
                    +------+------+
                           |
                  +--------v---------+
                  |  DashboardFrame  |
                  +--------+---------+
   +-----------+-----------+-----------+-----------+
   |           |           |           |           |
 Scanner     Batch      History     ML Trainer  Simulation
   |           |           |           |
   +-----+-----+           |           |
         |                 |           |
 FeatureExtractor      DataBaseConnection     MLModel
   (rules)              |           |       (Logistic Regression)
                    users.txt  scan_history.tsv


# Project Structure
File	Responsibility
Main.java	Entry point. Applies UI settings, tries to load a pre-trained ML model, launches the login screen
LoginFrame.java	Login UI, routing to registration, credential checks
DashboardFrame.java	Main window with all tabs; orchestrates background scanning
DataBaseConnection.java	Data access layer: reads/writes text files, hashes passwords, saves and retrieves scan history
FeatureExtractor.java	Rules engine: splits a URL into scheme, host and path and applies heuristic tests
MLModel.java	Logistic Regression from scratch: dataset parsing, feature vectors, training, saving, predictions
MLPanel.java	UI for loading a CSV, training, viewing metrics, and manual testing
BentoTheme.java	Central color palette and typography
BentoCard.java	Custom rounded-corner panel with consistent padding.


# Method 1: Rule-Based Checklist

The program asks a series of questions about the URL. Every "yes" adds risk points, and the more points a URL collects, the more dangerous it looks.

Feature	Points	Why it matters
Long URL (over 75 characters)	10	Long URLs often hide malicious domains
IP address as host	25	Legitimate sites use DNS names; raw IPs bypass filters
@ symbol in URL	15	Browsers ignore text before @, so attackers can fake a trusted domain
No HTTPS	10	Missing SSL suggests an untrusted or throwaway site
Many subdomains (3 or more)	10	e.g. login.paypal.com.secureupdate.tk
Suspicious keywords	up to 15	Words like login, secure, verify, billing, kyc, password
Risky TLD	12	Cheap or free domains such as .tk, .ml, .xyz, .zip
Punycode (xn--)	15	Look-alike foreign characters used in homograph attacks
Brand impersonation	20	Domain mentions "google" or "paypal" but belongs to another root domain
Trusted domain	-100	Known safe sites (e.g. github.com) get a big reduction so they are not wrongly flagged.

# Method 2: Machine Learning Model
Instead of fixed rules, the model learns from examples. You give it a dataset (e.g. phishing_site_urls.csv) of thousands of URLs, each labeled phishing (1) or legitimate (0). For each URL, the program converts the link into 24 numbers (length, dot count, keyword hits, and so on) and learns which numbers tend to go with phishing.
## How it predicts
Convert the new URL into the same 24 numbers
Normalize them with the saved mean and standard deviation
Multiply by the learned weights and add the bias
Pass the result through the sigmoid function:

# Login and Security

Every user has an account, so each person sees only their own history. Passwords are never stored in plain text.
Data Storage.

All data lives in plain files managed by DataBaseConnection.java. Every read and write is wrapped in synchronized (LOCK) so background threads cannot collide.

# Data Storage
This project uses .txt / .tsv files for simplicity:
Reason	   Explanation
No setup	Nothing extra to install; it runs anywhere Java runs
Portable	Copy the folder to another computer and it works
Transparent	Files are human-readable, which is good for learning and demos
Enough for the job	The amount of data is small.

## Summary

The system takes a URL, checks it against a list of known phishing tricks, and also asks a machine learning model that learned from thousands of real examples. It then shows a clear verdict. It supports multiple users with secure logins, keeps each user's scan history with charts, and is built in pure Java with no outside libraries or database servers, so it is easy to run, understand and explain.
