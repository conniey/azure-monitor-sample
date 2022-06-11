# Azure Monitor Query Sample

## Usage

1. Clone repository locally.
2. `mvn package`
3. `java -jar ./target/azure-monitor-sample-1.0-SNAPSHOT-jar-with-dependencies.jar -w <WORKSPACE-ID> -s <START-DATE> -e <END-DATE> -dc TimeGenerated -q "AppTraces | order by TimeGenerated asc`
   * `-w`: The workspace id
   * `-s`: The start date in UTC. Format: 2022-06-03T00:00:30Z
   * `-e`: The end date in UTC. Format: 2022-06-03T22:00:30Z
   * `-dc`: Name of the time column. Used to extract next query date. (ie. TimeGenerated)
   * `-q`: The Azure Monitor query. **NOTE: Order results by time, ascending.**
4. Output should be in the `logs/` folder.

## More information

To get full help menu, run `java -jar ./target/azure-monitor-sample-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## Example:  Querying Event Hubs logs in an Azure Monitor Workspace

To query and parse logs from `azure-messaging-eventhubs` (any version after `5.11.0`, when structured logging was added).

* Workspace id: af8a1455-ec02-4692-8691-347e446c7364
* Start date: 2022-06-03T00:00:30Z
* End date: 2022-06-03T22:00:30Z
* Date column: TimeGenerated
* Query:
  > Note that the results are sorted by time. Also, that the time column is included in results.
  ```
  AppTraces 
  | where Properties !has "PartitionPumpManager"
  | where Properties has "LoggerName" and Properties has_cs "com.azure"
  | project TimeGenerated, Message, Properties
  | extend m = parse_json(Message)
  | extend p = parse_json(Properties)
  | project TimeGenerated, Thread=p.ThreadName, Logger=p.LoggerName, Log=m["az.sdk.message"], ConnectionId=m.connectionId, EntityPath=m.entityPath, LinkName=m.linkName, Message
  | order by TimeGenerated asc
  ```
### Commands

In Powershell:

```powershell
# Setting variables
$workspaceId = "af8a1455-ec02-4692-8691-347e446c7364";
$startDate = "2022-06-03T00:00:30Z";
$endDate = "2022-06-03T22:00:30Z";
$dateColumn = "TimeGenerated";
$query = @"
AppTraces 
| where Properties !has "PartitionPumpManager"
| where Properties has "LoggerName" and Properties has_cs "com.azure"
| project TimeGenerated, Message, Properties
| extend m = parse_json(Message)
| extend p = parse_json(Properties)
| project TimeGenerated, Thread=p.ThreadName, Logger=p.LoggerName, Log=m["az.sdk.message"], ConnectionId=m.connectionId, EntityPath=m.entityPath, LinkName=m.linkName, Message
| order by TimeGenerated asc
"@;

# After variables are set, execute the command
java -jar ./target/azure-monitor-sample-1.0-SNAPSHOT-jar-with-dependencies.jar -s $startDate -e $endDate -w $workspaceId -q $query -dc $dateColumn
```