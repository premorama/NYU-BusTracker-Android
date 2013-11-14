package com.palsulich.nyubustracker.helpers;

import android.content.Context;
import android.util.Log;

import com.palsulich.nyubustracker.activities.MainActivity;
import com.palsulich.nyubustracker.models.Bus;
import com.palsulich.nyubustracker.models.Route;
import com.palsulich.nyubustracker.models.Stop;
import com.palsulich.nyubustracker.models.Time;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public final class BusManager {
    private static BusManager sharedBusManager = null;
    private static ArrayList<Stop> stops = null;
    private static ArrayList<Route> routes = null;
    private static ArrayList<String> hideRoutes = null;
    private static ArrayList<Bus> buses = null;
    private static Context mContext = null;

    public static BusManager getBusManager(Context context) {
        if (sharedBusManager == null) {
            sharedBusManager = new BusManager(context);
        }
        return sharedBusManager;
    }
    public static BusManager getBusManager() {
        if (sharedBusManager == null) {
            sharedBusManager = new BusManager(mContext);
        }
        return sharedBusManager;
    }

    private BusManager(Context context) {
        mContext = context;
        stops = new ArrayList<Stop>();
        routes = new ArrayList<Route>();
        hideRoutes = new ArrayList<String>();
        buses = new ArrayList<Bus>();
    }

    public ArrayList<Stop> getStops() {
        return stops;
    }

    public boolean hasStops() {
        return stops.size() > 0;
    }

    public Stop getStopByName(String stopName) {
        for (Stop s : stops) {
            if (s.getName().equals(stopName)) return s;
        }
        return null;
    }

    public Stop getStopByID(String stopID) {
        for (Stop s : stops) {
            if (s.getID().equals(stopID)) return s;
        }
        return null;
    }

    public String[] getStopsAsArray() {
        String[] stopsArray = new String[stops.size()];
        for (int i = 0; i < stopsArray.length; i++) {
            stopsArray[i] = stops.get(i).toString();
        }
        return stopsArray;
    }

    public ArrayList<Stop> getStopsByRouteID(String routeID) {
        ArrayList<Stop> result = new ArrayList<Stop>();
        for (Stop stop : stops) {
            //Log.v("Debugging", "Number of routes of stop " + j + ": " + stop.routes.size());
            if (stop.hasRouteByString(routeID)) {
                result.add(stop);
            }
        }
        return result;
    }

    public boolean hasRoutes() {
        return routes.size() > 0;
    }

    public String[] getRoutesAsArray() {
        String[] routesArray;
        if (routes.size() == 0) {
            routesArray = new String[1];
            routesArray[0] = "No routes available";
        } else {
            routesArray = new String[routes.size()];
        }
        for (int i = 0; i < routesArray.length; i++) {
            routesArray[i] = routes.get(i).toString();
        }
        return routesArray;
    }

    public Route getRouteByID(String id) {
        for (Route route : routes) {
            if (route.getID().equals(id)) {
                return route;
            }
        }
        return null;
    }

    public Route getRouteByName(String name) {
        for (Route route : routes) {
            if (route.getLongName().equals(name)) {
                return route;
            }
        }
        return null;
    }

    public String[] getConnectedStops(Stop stop){
        int resultSize = 0;
        String temp[] = new String[64];
        ArrayList<Route> stopRoutes = stop.getRoutes();
        for (Route route : stopRoutes) {       // For every route servicing this stop:
            String routeStops[] = route.getStopsAsArray();
            for (String connectedStop : routeStops){    // add all of that route's stops.
                if (!connectedStop.equals(stop.getName())){
                    temp[resultSize++] = connectedStop;
                }
            }
        }
        String result[] = new String[resultSize];
        System.arraycopy(temp, 0, result, 0, resultSize);
        return result;
    }

    public void addStop(Stop stop) {
        stops.add(stop);
        Log.v("Debugging", "Added " + stop.toString() + " to list of stops (" + stops.size() + ")");
    }

    public void addRoute(Route route) {
        if (!hideRoutes.contains(route.getID())){
            Log.v("JSONDebug", "Adding route: " + route.getID());
            routes.add(route);
        }
    }

    public void addBus(Bus bus) {
        buses.add(bus);
    }

    public Context getContext(){
        return mContext;
    }

    public static void parseTimes(JSONObject versionJson, FileGrabber mFileGrabber) throws JSONException {
        ArrayList<Stop> stops = sharedBusManager.getStops();
        Log.v("Debugging", "Looking for times for " + stops.size() + " stops.");
        JSONArray jHides = versionJson.getJSONArray("hideroutes");
        for (int j = 0; j < jHides.length(); j++){
            String hideMeID = jHides.getString(j);
            Log.v("JSONDebug", "Hiding a route... " + hideMeID);
            Route r = sharedBusManager.getRouteByID(hideMeID);
            if (r != null){
                routes.remove(r);
                for (Stop s : stops){
                    if (s.hasRouteByString(hideMeID)){
                        s.getRoutes().remove(r);
                        Log.v("JSONDebug", "Removing route " + r.getID() + " from " + s.getName());
                    }
                }
            }
            else{
                hideRoutes.add(hideMeID);
            }
        }

        JSONArray jVersion = versionJson.getJSONArray("versions");
        for (int j = 0; j < jVersion.length(); j++) {
            JSONObject stopObject = jVersion.getJSONObject(j);
            String file = stopObject.getString("file");
            Log.v("Debugging", "Looking for times for " + file);
            JSONObject timesJson = mFileGrabber.getTimesFromFile(file);
            JSONObject routes = timesJson.getJSONObject(MainActivity.TAG_ROUTES);
            Stop s = sharedBusManager.getStopByID(file.substring(0, file.indexOf(".")));
            for (int i = 0; i < s.getRoutes().size(); i++) {
                if (routes.has(s.getRoutes().get(i).getID())) {
                    JSONObject routeTimes = routes.getJSONObject(s.getRoutes().get(i).getID());
                    if (routeTimes.has(MainActivity.TAG_WEEKDAY)) {
                        JSONArray weekdayTimesJson = routeTimes.getJSONArray(MainActivity.TAG_WEEKDAY);
                        Time[] weekdayTimes = new Time[weekdayTimesJson.length()];
                        Log.v("Debugging", "Found " + weekdayTimes.length + " weekday times.");
                        if (weekdayTimesJson != null) {
                            for (int k = 0; k < weekdayTimes.length; k++) {
                                weekdayTimes[k] = new Time(weekdayTimesJson.getString(k));
                            }
                            String weekdayRoute = routeTimes.getString(MainActivity.TAG_ROUTE);
                            s.addTime(weekdayRoute.substring(weekdayRoute.indexOf("Route ") + "Route ".length()), "Weekday", weekdayTimes);
                        }
                    }
                    if (routeTimes.has(MainActivity.TAG_FRIDAY)) {
                        JSONArray fridayTimesJson = routeTimes.getJSONArray(MainActivity.TAG_FRIDAY);
                        Time[] fridayTimes = new Time[fridayTimesJson.length()];
                        if (fridayTimesJson != null) {
                            for (int k = 0; k < fridayTimes.length; k++) {
                                fridayTimes[k] = new Time(fridayTimesJson.getString(k));
                            }
                            String fridayRoute = routeTimes.getString(MainActivity.TAG_ROUTE);
                            s.addTime(fridayRoute.substring(fridayRoute.indexOf("Route ") + "Route ".length()), "Friday", fridayTimes);

                        }
                    }
                    if (routeTimes.has(MainActivity.TAG_WEEKEND)) {
                        JSONArray weekendTimesJson = routeTimes.getJSONArray(MainActivity.TAG_WEEKEND);
                        Time[] weekendTimes = new Time[weekendTimesJson.length()];
                        if (weekendTimesJson != null) {
                            for (int k = 0; k < weekendTimes.length; k++) {
                                weekendTimes[k] = new Time(weekendTimesJson.getString(k));
                            }
                            String weekendRoute = routeTimes.getString(MainActivity.TAG_ROUTE);
                            s.addTime(weekendRoute.substring(weekendRoute.indexOf("Route ") + "Route ".length()), "Weekend", weekendTimes);

                        }
                    }
                }
            }

        }
    }

}
