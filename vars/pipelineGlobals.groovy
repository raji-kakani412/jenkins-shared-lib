def getAccountID(String environment){
    switch(environment){
        case 'dev':
           return "give dev account id here"
        case 'qa':
           return "give qa account id here"
        case 'uat':
           return "give uat account id here"
        case 'pre-prod':
           return "give pre-prod account id here"
        case 'prod':
           return "give prod account id here"
        default:
            return "nothing"     
        


    }
}