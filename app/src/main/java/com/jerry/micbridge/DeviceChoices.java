package com.jerry.micbridge;

import java.util.*;

/** Immutable selector rows. Device identity is never inferred from a product name or position. */
public final class DeviceChoices {
    public static final class Row {
        public final int id;
        public final String name, connection;
        public final boolean builtIn;
        private final String label;
        public Row(int id, String name, String connection, boolean builtIn) {
            this(id, name, connection, builtIn, name + " · " + connection);
        }
        private Row(int id,String name,String connection,boolean builtIn,String label) {
            this.id=id;this.name=name;this.connection=connection;this.builtIn=builtIn;this.label=label;
        }
        @Override public String toString(){return label;}
    }
    public static List<Row> build(Collection<Row> snapshot,String prompt) {
        Map<Integer,Row> unique=new TreeMap<>();
        for(Row row:snapshot)if(row.id>=0&&!unique.containsKey(row.id))unique.put(row.id,row);
        List<Row> sorted=new ArrayList<>(unique.values());
        Collections.sort(sorted,(a,b)->{
            int c=Boolean.compare(a.builtIn,b.builtIn);
            if(c==0)c=a.toString().compareToIgnoreCase(b.toString());
            return c==0?Integer.compare(a.id,b.id):c;
        });
        Map<String,Integer> totals=new HashMap<>(),seen=new HashMap<>();
        for(Row row:sorted)totals.put(row.label,totals.containsKey(row.label)?totals.get(row.label)+1:1);
        List<Row> result=new ArrayList<>();result.add(new Row(-1,prompt,"",false,prompt));
        for(Row row:sorted){
            int number=seen.containsKey(row.label)?seen.get(row.label)+1:1;seen.put(row.label,number);
            String label=row.label+(totals.get(row.label)>1?" · Option "+number:"");
            result.add(new Row(row.id,row.name,row.connection,row.builtIn,label));
        }
        return Collections.unmodifiableList(result);
    }
    /** Missing devices always resolve to the prompt, never a different device. */
    public static int position(List<Row> rows,int selectedId){
        for(int n=1;n<rows.size();n++)if(rows.get(n).id==selectedId)return n;
        return 0;
    }
    private DeviceChoices(){}
}
