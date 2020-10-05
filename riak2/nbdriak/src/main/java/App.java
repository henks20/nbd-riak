import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.kv.UpdateValue;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import org.apache.log4j.BasicConfigurator;
import java.rmi.UnknownHostException;

public class App {

    public static class Zakupy {
        public String sklep;
        public int koszt;
        public boolean promocja;
    }

    public static class ZakupyUpdate extends UpdateValue.Update<Zakupy> {
        private final Zakupy zakupy;

        public ZakupyUpdate(Zakupy zakupy) {
            this.zakupy = zakupy;
        }

        @Override
        public Zakupy apply(Zakupy data) {
            if (data == null) {
                data = new Zakupy();
            }

            data.koszt = zakupy.koszt;
            data.promocja = zakupy.promocja;
            data.sklep = zakupy.sklep;

            return data;
        }
    }

        private static RiakCluster setUpCluster() throws UnknownHostException {
            RiakNode riaknode = new RiakNode.Builder()
                    .withRemoteAddress("127.0.0.1")
                    .withRemotePort(8087)
                    .build();
            RiakCluster riakcluster = new RiakCluster.Builder(riaknode)
                    .build();

            riakcluster.start();
            return riakcluster;
        }

        public static void main(String[] args) {
            BasicConfigurator.configure();
            try {
                RiakCluster riakcluster = setUpCluster();
                RiakClient riakclient = new RiakClient(riakcluster);

                Zakupy zakupy = new Zakupy();
                zakupy.sklep = "Biedronka";
                zakupy.promocja = true;
                zakupy.koszt = 122;

                Namespace zakupyBucket = new Namespace("zakupy");
                Location zakupyLocation = new Location(zakupyBucket, "zakupy");
                StoreValue storeZakupy = new StoreValue.Builder(zakupy)
                        .withLocation(zakupyLocation)
                        .build();

                riakclient.execute(storeZakupy);
                System.out.println("### ZAPISANO ZAKUPY W BAZIE");
                FetchValue fetchZakupy = new FetchValue.Builder(zakupyLocation)
                        .build();

                Zakupy fetchedZakupy = riakclient.execute(fetchZakupy).getValue(Zakupy.class);
                System.out.println("### POBRANO ZAKUPY Z BAZY");
                System.out.println("### Zakupy w: " + fetchedZakupy.sklep + " ,o koszcie: "
                        + fetchedZakupy.koszt + " ,czy z promocja: " + fetchedZakupy.promocja);

                assert (zakupy.getClass() == fetchedZakupy.getClass());

                zakupy.koszt = 999;
                ZakupyUpdate nowyKoszt = new ZakupyUpdate(zakupy);
                UpdateValue updateValue = new UpdateValue.Builder(zakupyLocation)
                        .withUpdate(nowyKoszt)
                        .build();

                UpdateValue.Response response = riakclient.execute(updateValue);
                System.out.println("### ZAKTUALIZOWANO ZAKUPY W BAZIE");
                fetchedZakupy = riakclient.execute(fetchZakupy).getValue(Zakupy.class);
                System.out.println("### POBRANO ZAKTUALIZOWANE ZAKUPY Z BAZY");
                System.out.println("### Zakupy zaktualizowane w: " + fetchedZakupy.sklep + " ,o koszcie: "
                        + fetchedZakupy.koszt + " ,czy z promocja: " + fetchedZakupy.promocja);

                DeleteValue deleteZakupy = new DeleteValue.Builder(zakupyLocation)
                        .build();
                riakclient.execute(deleteZakupy);
                System.out.println("### USUNIETO ZAKUPY Z BAZY");
                fetchedZakupy = riakclient.execute(fetchZakupy).getValue(Zakupy.class);
                System.out.println("### SPROBOWANO POBRAC USUNIETE ZAKUPY Z BAZY");
                System.out.println("### Zakupy usuniete w: " + fetchedZakupy.sklep + " ,o koszcie: "
                        + fetchedZakupy.koszt + " ,czy z promocja: " + fetchedZakupy.promocja);
                System.out.println("### POWINNO LINIJKE WYZEJ WYDRUKOWAC USUNIETE ZAKUPY");
                riakcluster.shutdown();
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }
        }
    }


