package pathXmlParser;

import java.util.HashMap;
import java.util.Map;

public class TypeNumbersConstants {
    public final Map<String, Integer> typeNumberMap = new HashMap<>();

    TypeNumbersConstants(){
        typeNumberMap.put("addresses/address", 1);
        typeNumberMap.put("cafSendingSourceRequiredAddresses/address", 2);
        typeNumberMap.put("cafReceivingDestinationRequiredAddresses/address", 3);
        typeNumberMap.put("cafAddresses/address", 4);
        typeNumberMap.put("cakAddresses/address", 5);
        typeNumberMap.put("cryptAddresses/address", 6);
        typeNumberMap.put("sendingSources/sendingSource/destination/address", 7);
        typeNumberMap.put("sendingSources/sendingSource/sdsDestination/address", 8);
        typeNumberMap.put("receivingDestinations/receivingDestination/queueDestination/address", 9);
        typeNumberMap.put("receivingDestinations/receivingDestination/cafRequiredAddresses/address", 10);
        typeNumberMap.put("receivingDestinations/receivingDestination/address", 11);
        typeNumberMap.put("sendingSources/sendingSource/actions/cafRequiredAddresses", 12);

        typeNumberMap.put("fileSets/fileSet", 1);
        typeNumberMap.put("receivingDestinations/receivingDestination/fileSet", 2);
        typeNumberMap.put("sendingSources/sendingSource/fileSet/fileSet", 3);

        typeNumberMap.put("receivingDestinations/receivingDestination/executes/execute", 1);

        typeNumberMap.put("receivingDestinations/receivingDestination", 0);
        typeNumberMap.put("receivingDestinations/defaultDestination", 1);
        typeNumberMap.put("receivingDestinations/junkDestination", 2);

        typeNumberMap.put("hosts/router", 1);
        typeNumberMap.put("hosts/host", 1);
        typeNumberMap.put("sdsDestination/hosts/router", 2);
        typeNumberMap.put("sdsDestination/hosts/host", 2);
    }
}
