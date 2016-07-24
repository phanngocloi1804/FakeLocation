package fake.walking.gps.pokemon.go;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by loipn on 7/24/2016.
 */
public class Address {

    private String name;
    private LatLng latLng;

    public Address(String name, LatLng latLng) {
        this.name = name;
        this.latLng = latLng;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }
}
