import com.jerry.micbridge.DeviceChoices;
import java.util.*;
public final class DeviceChoicesTest {
    private static DeviceChoices.Row row(int id,String name,boolean builtIn){return new DeviceChoices.Row(id,name,builtIn?"Built-in":"USB",builtIn);}
    private static void check(boolean pass,String message){if(!pass)throw new AssertionError(message);}
    public static void main(String[] args){
        List<DeviceChoices.Row> raw=Arrays.asList(row(20,"Phone microphone",true),row(8,"USB mic",false),row(8,"USB mic",false),row(21,"Phone microphone",true),row(9,"USB mic",false));
        List<DeviceChoices.Row> rows=DeviceChoices.build(raw,"Choose microphone");
        check(rows.size()==5,"deduplicate repeated IDs, preserve distinct same-name ports");
        check(rows.get(0).id==-1,"explicit unselected state");
        check(rows.get(1).id==8&&rows.get(2).id==9,"external routes first with stable ordering");
        Set<String> labels=new HashSet<>();for(DeviceChoices.Row r:rows)check(labels.add(r.toString()),"ambiguous labels");
        List<DeviceChoices.Row> shuffled=new ArrayList<>(raw);Collections.reverse(shuffled);
        List<DeviceChoices.Row> reversed=DeviceChoices.build(shuffled,"Choose microphone");
        for(int n=0;n<rows.size();n++)check(rows.get(n).id==reversed.get(n).id&&rows.get(n).toString().equals(reversed.get(n).toString()),"snapshot order changed identity or label");
        check(DeviceChoices.position(rows,21)==4,"restore by ID");
        List<DeviceChoices.Row> removed=DeviceChoices.build(Arrays.asList(row(9,"USB mic",false),row(20,"Phone microphone",true)),"Choose microphone");
        check(DeviceChoices.position(removed,8)==0,"disconnect must not silently select replacement");
        check(DeviceChoices.position(removed,20)==2,"surviving selection remains valid");
        check(DeviceChoices.position(removed,-1)==0&&DeviceChoices.position(removed,999)==0,"unknown selections stay unselected");
        check(DeviceChoices.build(Collections.emptyList(),"No device").size()==1,"empty snapshot");
        try{rows.clear();throw new AssertionError("mutable rows");}catch(UnsupportedOperationException expected){}
        System.out.println("DeviceChoicesTest passed: duplicates, distinct ports, stable labels/order, disconnect and immutable snapshots");
    }
}
