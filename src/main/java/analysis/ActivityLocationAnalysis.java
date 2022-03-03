package analysis;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.w3.xlink.Simple;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class ActivityLocationAnalysis implements ActivityStartEventHandler {

    List<String> actTypes = new ArrayList<>();
    Map<Coord, String> actCoords = new HashMap<>();
    Map<Integer, String> typesEvent = new HashMap<>();
    Map<Integer, String> typesMap = new HashMap<>();
    Integer i = 0;
    static Geometry consideredArea = null;


    public static void main(String args[]) {

        String eventsFile = "C:/Users/Simon/Desktop/062.output_events.xml.gz";
        String outputFile = "C:/Users/Simon/Desktop/actCoords.csv";
        String outputFile2 = "C:/Users/Simon/Desktop/actTypesComp.csv";
        String consideredAreaShpFile = "C:/Users/Simon/Documents/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1/input/leipzig-utm32n/leipzig-utm32n.shp";

        ShapeFileReader shpReader = new ShapeFileReader();
        Collection<SimpleFeature> features = shpReader.readFileAndInitialize(consideredAreaShpFile);

        for(SimpleFeature feature : features) {
            if(consideredArea == null) {
                consideredArea = (Geometry) feature.getDefaultGeometry();
            } else {
                consideredArea = consideredArea.union((Geometry) feature.getDefaultGeometry());
            }
        }

        EventsManager manager = EventsUtils.createEventsManager();

        ActivityLocationAnalysis handler = new ActivityLocationAnalysis();
        manager.addHandler(handler);
        manager.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(manager);
        reader.readFile(eventsFile);
        manager.finishProcessing();
        handler.writeActivityLocations(outputFile , outputFile2);
    }



    @Override
    public void handleEvent(ActivityStartEvent event) {
        //first check which act types exist
        //check for act type (we only want to look at educ, home, work, business, leisure...)
        //actually it should be easier to judt exclude the ones we dont want: XX interaction, freight.., other
        //if one of the aforementioned: get act location -> if loc isnt in map already -> add
        //print map to csv
        if(MGC.coord2Point(event.getCoord()).within(consideredArea)) {

            if(!(event.getActType().contains("interaction") || event.getActType().contains("freight") || event.getActType().contains("other"))) {
    //            this.actCoords.putIfAbsent(event.getCoord(), event.getActType());
                if(!this.actCoords.containsKey(event.getCoord())) {
                    this.actCoords.put(event.getCoord(), event.getActType());
                } else {
    //                System.out.println("TYPE: " + event.getActType() + " VS " + this.actCoords.get(event.getCoord()));
                    this.typesEvent.put(i, event.getActType());
                    this.typesMap.put(i, this.actCoords.get(event.getCoord()));
                    i++;
                }
            }
        }
    }

    void writeActivityLocations(String outputFile, String outputFile2) {
        BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
        BufferedWriter writer2 = IOUtils.getBufferedWriter(outputFile2);
        try {
            System.out.println("WRITING ACTIVITY LOCATIONS");
            writer.write("actType;Long-X;Lat-Y");
            writer.newLine();

            for (Coord coord  : this.actCoords.keySet()) {
                writer.write(this.actCoords.get(coord) + ";" + coord.getX() + ";" + coord.getY());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer2.write("no;actTypeEvent;actTypeMap");
            writer2.newLine();

            for (Integer j  : this.typesEvent.keySet()) {
                writer2.write(j + ";" + this.typesEvent.get(j) + ";" + this.typesMap.get(j));
                writer2.newLine();
            }
            writer2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
