package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.provider.BaseColumns;

import java.util.List;

@Entity(tableName = KanjiComponent.TABLE_NAME)
public class KanjiComponent {

    public static final String TABLE_NAME = "kanji_components_table";
    public static final String COLUMN_ID = BaseColumns._ID;
    static final String COLUMN_COMPONENT_STRUCTURE = "structure";
    private static final String COLUMN_COMPONENT_ASSOCIATED_COMPONENTS = "associatedComponents";

    public KanjiComponent() {
    }

    @Ignore
    public KanjiComponent(String structure) {
        this.structure = structure;
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(index = true, name = COLUMN_ID)
    public long id;
    public long getComponentId() {
        return id;
    }
    public void setComponentId(long component_id) {
        this.id = component_id;
    }

    @ColumnInfo(name = COLUMN_COMPONENT_STRUCTURE)
    private String structure;
    public String getStructure() {
        return structure;
    }
    public void setStructure(String structure) {
        this.structure = structure;
    }


    @TypeConverters({JapaneseToolboxDbTypeConverters.class})
    @ColumnInfo(name = COLUMN_COMPONENT_ASSOCIATED_COMPONENTS)
    private List<KanjiComponent.AssociatedComponent> associatedComponents;
    public void setAssociatedComponents(List<KanjiComponent.AssociatedComponent> associatedComponents) {
        this.associatedComponents = associatedComponents;
    }
    public List<KanjiComponent.AssociatedComponent> getAssociatedComponents() {
        return associatedComponents;
    }

    public static class AssociatedComponent {

        private String component;
        public void setComponent(String component) {
            this.component = component;
        }
        public String getComponent() {
            return component;
        }

        private String associatedComponents;
        public void setAssociatedComponents(String associatedComponents) {
            this.associatedComponents = associatedComponents;
        }
        public String getAssociatedComponents() {
            return associatedComponents;
        }
    }
}
