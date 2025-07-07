
package com.example.engine.generated.engines;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.processing.Generated;

@Generated("jsonschema2pojo")
public class EngineGas {

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
    public Long horsepower;
    public BigDecimal fuelEfficiency;
    public Set<String> fuelTypes = new LinkedHashSet<String>();
    /**
     * 
     * (Required)
     * 
     */
    public Set<UUID> carUuids = new LinkedHashSet<UUID>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EngineGas.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("uuid");
        sb.append('=');
        sb.append(((this.uuid == null)?"<null>":this.uuid));
        sb.append(',');
        sb.append("horsepower");
        sb.append('=');
        sb.append(((this.horsepower == null)?"<null>":this.horsepower));
        sb.append(',');
        sb.append("fuelEfficiency");
        sb.append('=');
        sb.append(((this.fuelEfficiency == null)?"<null>":this.fuelEfficiency));
        sb.append(',');
        sb.append("fuelTypes");
        sb.append('=');
        sb.append(((this.fuelTypes == null)?"<null>":this.fuelTypes));
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
        result = ((result* 31)+((this.fuelEfficiency == null)? 0 :this.fuelEfficiency.hashCode()));
        result = ((result* 31)+((this.carUuids == null)? 0 :this.carUuids.hashCode()));
        result = ((result* 31)+((this.horsepower == null)? 0 :this.horsepower.hashCode()));
        result = ((result* 31)+((this.uuid == null)? 0 :this.uuid.hashCode()));
        result = ((result* 31)+((this.fuelTypes == null)? 0 :this.fuelTypes.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof EngineGas) == false) {
            return false;
        }
        EngineGas rhs = ((EngineGas) other);
        return (((((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.fuelEfficiency == rhs.fuelEfficiency)||((this.fuelEfficiency!= null)&&this.fuelEfficiency.equals(rhs.fuelEfficiency))))&&((this.carUuids == rhs.carUuids)||((this.carUuids!= null)&&this.carUuids.equals(rhs.carUuids))))&&((this.horsepower == rhs.horsepower)||((this.horsepower!= null)&&this.horsepower.equals(rhs.horsepower))))&&((this.uuid == rhs.uuid)||((this.uuid!= null)&&this.uuid.equals(rhs.uuid))))&&((this.fuelTypes == rhs.fuelTypes)||((this.fuelTypes!= null)&&this.fuelTypes.equals(rhs.fuelTypes))));
    }

}
