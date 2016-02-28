package ru.cti.regexp.testtest;

/**
 * Created by 134 on 28.02.2016.
 */
public class TestObject {
    int field1;
    String field2;

    public TestObject(int field1, String field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestObject that = (TestObject) o;

        return field1 == that.field1;

    }

    @Override
    public int hashCode() {
        return field1;
    }

    @Override
    public String toString() {
        return "TestObject{" +
                "field1=" + field1 +
                ", field2='" + field2 + '\'' +
                '}';
    }
}
