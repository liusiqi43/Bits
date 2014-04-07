package com.siqi.bits_GDgen;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

public class BitsDaoGenerator {

    public static void main(String args[]) throws Exception {
            Schema schema = new Schema(3, "com.siqi.bits");
            Entity upload = schema.addEntity("Upload");
            upload.addIdProperty();
            upload.addStringProperty("text");
            new DaoGenerator().generateAll(schema, "bits/src-gen");
    }
}