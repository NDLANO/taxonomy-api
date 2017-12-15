# Build the project

taxonomy-api is a Spring Boot project. You need a working Java environment, Docker and Virtualbox. 

Once you've cloned the project, run `mvn clean install`. Set up the following environment variable: `SPRING_DATASOURCE_USERNAME="sa"`. 
The API uses Postgres in AWS. To test your code with a local Postgres database, run `./run_itest.sh`. 
This script will download a Docker Postgres image, set it up on Virtualbox and run the tests against a Postgres database.  

You can also connect your local taxonomy-API to the development databases in AWS. To do this you need to set a few environment variables: `PASSWORD` and `URL`. 
The variables should point to the AWS Postgres database (with port indication) you wish to connect to and the password for the database. 
Connect by running `./run.sh`.  
