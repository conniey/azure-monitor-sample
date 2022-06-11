# Azure Monitor Query Sample

## Prerequisites

1. Azure CLI
2. Maven

## Usage

1. Clone repository. Go to repository root.
2. `mvn package`
3. `az login`
4. `java -jar ./target/azure-monitor-sample-1.0-SNAPSHOT-jar-with-dependencies.jar -w <WORKSPACE-ID> -s <START-DATE> -e <END-DATE> -dc TimeGenerated -q "AppTraces | order by TimeGenerated asc`
   * `-w`: The workspace id
   * `-s`: The start date in UTC. Format: 2022-06-03T00:00:30Z
   * `-e`: The end date in UTC. Format: 2022-06-03T22:00:30Z
   * `-dc`: Name of the time column. Used to extract next query date. (ie. TimeGenerated)
   * `-q`: The Azure Monitor query. **NOTE: Order results by time, ascending.**
5. Output should be in the `logs/` folder.

## More information

To get full help menu, run `java -jar ./target/azure-monitor-sample-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## Example:  Querying Event Hubs logs in an Azure Monitor Workspace

To query and parse logs from `azure-messaging-eventhubs` (any version after `5.11.0`, when structured logging was added).

### Commands

In Powershell:

> NOTE: The query is ordered by time. Also, be aware of quotation marks. Single quotes (') are used in the query expression. 

```sh
# Setting variables
$workspaceId = "af8a1455-ec02-4692-8691-347e446c7364";
$startDate = "2022-06-03T00:00:30Z";
$endDate = "2022-06-03T22:00:30Z";
$dateColumn = "TimeGenerated";
$query = @"
AppTraces 
| where Properties !has 'PartitionPumpManager'
| where Properties has 'LoggerName' and Properties has_cs 'com.azure'
| project TimeGenerated, Message, Properties
| extend m = parse_json(Message)
| extend p = parse_json(Properties)
| project TimeGenerated, Thread=p.ThreadName, Logger=p.LoggerName, Log=m['az.sdk.message'], ConnectionId=m.connectionId, EntityPath=m.entityPath, LinkName=m.linkName, Message
| order by TimeGenerated asc
"@;

# After variables are set, execute the command
java -jar ./target/azure-monitor-sample-1.0-SNAPSHOT-jar-with-dependencies.jar -s $startDate -e $endDate -w $workspaceId -q $query -dc $dateColumn
```

Afterwards, the output should be in the `logs/` folder.