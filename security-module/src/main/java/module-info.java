module com.udacity.security {
    requires com.udacity.image;
    requires miglayout.swing;
    requires java.desktop;
    requires com.google.common;
    requires gson;
    requires java.prefs;
    requires java.sql;
    requires jdk.unsupported;
    opens com.udacity.security.data to gson;
}
