# Spring cloud config client+server example using git+vault as storage + RabbitMQ to broadcast config changes to clients

This is example project showcasing [Spring cloud config server](https://cloud.spring.io/spring-cloud-config/multi/multi__spring_cloud_config_server.html) with spring app as its client. 

* Configurations are stored in git and secrets in [Vault](https://www.vaultproject.io/)
* Config changes are propagated in network using [RabbitMQ](https://www.rabbitmq.com/) as [Spring Cloud Bus](http://cloud.spring.io/spring-cloud-bus/)

Requirements:
* Java 8
* Docker (including docker-compose)


## Try it out yourself

Relevant links:
* Config server - http://localhost:8081
* Client app example endpoint - http://localhost:8080/hello
* Vault UI - http://localhost:8200/ui
* RabbitMQ - http://localhost:15672 (guest/guest)

1. Start Vault and RabbitMQ containers `docker-compose up`
2. Configure secret for Client application in Vault
    
    1. Open [http://localhost:9200/ui] in browser. Enter 1 for 'Key Shares' and 'Key Threshold' values. Proceed with 'Initialize'
    2. Mark 'INITIAL ROOT TOKEN' and 'KEY 1' down somewhere. You need those tokens/keys in next steps. Proceed with 'Continue unsealing'
    3. Unseal Vault wiht out single key. Paste 'KEY 1' value to 'Key' and proceed wiht 'Unseal'
    4. Now login with your 'INITIAL ROOT TOKEN'
    5. Open 'Secret' menupoint and open 'secret/' from table.
    6. Add new secret for our client application by clicking 'Create Secret' and filling:
        * 'PATH FOR THIS SECRET' = 'config-client'
        * 'Key' - 'secret.from.vault' = 'Property value loaded from Vault'
        Do not forget to click 'Add' and 'Save'. You are finished with Vault
        
    or same steps from command line
    ```bash
    docker exec -it vault.server ash
    vault operator init -address=http://127.0.0.1:8200 # copy 'INITIAL ROOT TOKEN' and 'KEY 1-3'
    vault operator unseal -address=http://127.0.0.1:8200
    vault operator unseal -address=http://127.0.0.1:8200
    vault operator unseal -address=http://127.0.0.1:8200
    
    export VAULT_TOKEN=3c4002d2-456e-9dd2-399c-6c8e058e8050
    
    vault write -address=http://127.0.0.1:8200 secret/config-client secret.from.vault="Property value loaded from Vault"
    ```
    
3. Start config server `./mvnw -pl module_server spring-boot:run`
4. Start client application
    ```bash
    export VAULT_TOKEN=3c4002d2-456e-9dd2-399c-6c8e058e8050 # this should be value of 'INITIAL ROOT TOKEN'
    ./mvnw -pl module_client spring-boot:run
    ```
5. Check client configuration `http://localhost:8080/hello` note the response
    ```json
    {
    "spring.datasource.url": "jdbc:mysql://someserver-dev:1521/db",
    "secret.from.vault": "Property value loaded from Vault"
    }
    ``` 
6. Check cloud config server configuration for client application `http://localhost:8081/config-client/development`
    ```bash
    curl -X "GET" "http://localhost:8081/config-client/development" -H "X-Config-Token: $VAULT_TOKEN"
    ```
    
    Response should be something similar:
    ```json
    {
        "name": "config-client",
        "profiles": [
            "development"
        ],
        "label": null,
        "version": null,
        "state": null,
        "propertySources": [
            {
                "name": "https://github.com/toimtoimtoim/ci-spring-cloud-config-git-vault-rabbitmq.git/config_repo/config-client-development.properties",
                "source": {
                    "spring.datasource.url": "jdbc:mysql://someserver-dev:1521/db"
                }
            },
            {
                "name": "https://github.com/toimtoimtoim/ci-spring-cloud-config-git-vault-rabbitmq.git/config_repo/config-client.properties",
                "source": {
                    "spring.datasource.url": "jdbc:mysql://someserver-live:1521/db"
                }
            },
            {
                "name": "vault:config-client",
                "source": {
                    "secret.from.vault": "Property value loaded from Vault"
                }
            }
        ]
    }
    ```

7. Change 'spring.datasource.url=' value  in `config_repo/config-client.properties` file.
8. Trigger config reload event in config server `curl -X POST http://localhost:8081/actuator/bus-refresh`
9. Check that config change has propagated to the client app `http://localhost:8080/hello`
10. Open Vault [http://localhost:9200/ui] and change `secret/config-client` key 'secret.from.vault' value to something different
11. Trigger config reload event in config server `curl -X POST http://localhost:8081/actuator/bus-refresh`
12. Check that config change has propagated to the client app `http://localhost:8080/hello`