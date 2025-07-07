
package com.example.engine.generated.cars;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.processing.Generated;

@Generated("jsonschema2pojo")
public class CarSedan {

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
    public String model;
    public Long maxSpeed;
    public Set<String> features = new LinkedHashSet<String>();
    /**
     * 
     * (Required)
     * 
     */
    public Set<String> engineUuids = new LinkedHashSet<String>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CarSedan.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("uuid");
        sb.append('=');
        sb.append(((this.uuid == null)?"<null>":this.uuid));
        sb.append(',');
        sb.append("model");
        sb.append('=');
        sb.append(((this.model == null)?"<null>":this.model));
        sb.append(',');
        sb.append("maxSpeed");
        sb.append('=');
        sb.append(((this.maxSpeed == null)?"<null>":this.maxSpeed));
        sb.append(',');
        sb.append("features");
        sb.append('=');
        sb.append(((this.features == null)?"<null>":this.features));
        sb.append(',');
        sb.append("engineUuids");
        sb.append('=');
        sb.append(((this.engineUuids == null)?"<null>":this.engineUuids));
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
        result = ((result* 31)+((this.features == null)? 0 :this.features.hashCode()));
        result = ((result* 31)+((this.model == null)? 0 :this.model.hashCode()));
        result = ((result* 31)+((this.maxSpeed == null)? 0 :this.maxSpeed.hashCode()));
        result = ((result* 31)+((this.uuid == null)? 0 :this.uuid.hashCode()));
        result = ((result* 31)+((this.engineUuids == null)? 0 :this.engineUuids.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CarSedan) == false) {
            return false;
        }
        CarSedan rhs = ((CarSedan) other);
        return (((((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.features == rhs.features)||((this.features!= null)&&this.features.equals(rhs.features))))&&((this.model == rhs.model)||((this.model!= null)&&this.model.equals(rhs.model))))&&((this.maxSpeed == rhs.maxSpeed)||((this.maxSpeed!= null)&&this.maxSpeed.equals(rhs.maxSpeed))))&&((this.uuid == rhs.uuid)||((this.uuid!= null)&&this.uuid.equals(rhs.uuid))))&&((this.engineUuids == rhs.engineUuids)||((this.engineUuids!= null)&&this.engineUuids.equals(rhs.engineUuids))));
    }

}
