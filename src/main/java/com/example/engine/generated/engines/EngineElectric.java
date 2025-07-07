
package com.example.engine.generated.engines;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.processing.Generated;

@Generated("jsonschema2pojo")
public class EngineElectric {

    /**
     * 
     * (Required)
     * 
     */
    public String name;
    /**
     * 
     * (Required)
     * 
     */
    public UUID uuid;
    /**
     * 
     * (Required)
     * 
     */
    public Long batteryCapacity;
    public Long rangeMiles;
    public Set<String> chargingTypes = new LinkedHashSet<String>();
    /**
     * 
     * (Required)
     * 
     */
    public Set<UUID> carUuids = new LinkedHashSet<UUID>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EngineElectric.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("uuid");
        sb.append('=');
        sb.append(((this.uuid == null)?"<null>":this.uuid));
        sb.append(',');
        sb.append("batteryCapacity");
        sb.append('=');
        sb.append(((this.batteryCapacity == null)?"<null>":this.batteryCapacity));
        sb.append(',');
        sb.append("rangeMiles");
        sb.append('=');
        sb.append(((this.rangeMiles == null)?"<null>":this.rangeMiles));
        sb.append(',');
        sb.append("chargingTypes");
        sb.append('=');
        sb.append(((this.chargingTypes == null)?"<null>":this.chargingTypes));
        sb.append(',');
        sb.append("carUuids");
        sb.append('=');
        sb.append(((this.carUuids == null)?"<null>":this.carUuids));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.carUuids == null)? 0 :this.carUuids.hashCode()));
        result = ((result* 31)+((this.batteryCapacity == null)? 0 :this.batteryCapacity.hashCode()));
        result = ((result* 31)+((this.rangeMiles == null)? 0 :this.rangeMiles.hashCode()));
        result = ((result* 31)+((this.uuid == null)? 0 :this.uuid.hashCode()));
        result = ((result* 31)+((this.chargingTypes == null)? 0 :this.chargingTypes.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EngineElectric) == false) {
            return false;
        }
        EngineElectric rhs = ((EngineElectric) other);
        return (((((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.carUuids == rhs.carUuids)||((this.carUuids!= null)&&this.carUuids.equals(rhs.carUuids))))&&((this.batteryCapacity == rhs.batteryCapacity)||((this.batteryCapacity!= null)&&this.batteryCapacity.equals(rhs.batteryCapacity))))&&((this.rangeMiles == rhs.rangeMiles)||((this.rangeMiles!= null)&&this.rangeMiles.equals(rhs.rangeMiles))))&&((this.uuid == rhs.uuid)||((this.uuid!= null)&&this.uuid.equals(rhs.uuid))))&&((this.chargingTypes == rhs.chargingTypes)||((this.chargingTypes!= null)&&this.chargingTypes.equals(rhs.chargingTypes))));
    }

}
