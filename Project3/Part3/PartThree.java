package Project3.Part3;

import Project3.CityData;
import Project3.CityInfo;
import Project3.CostEdge;
import Project3.DjikstraRow;
import Project3.TravelMatrix;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

/**
 * CS6423 Algorithmic Processes
 * Summer 2014
 * Project 3: PartThree Djikstra/Yen's implementation
 * Daniel Kerr and Charles So
 * Date: 08/01/2014
 * File: PartThree.java
 */
public class PartThree {

    private CityInfo[] cityInfo;
    private float[][] costMatrix;
    private String[][] carrierMatrix;

    public PartThree() {
        // Read in the city data
        CityData newCityData = new CityData("routes.txt");

        cityInfo = newCityData.getListOfCities();
        costMatrix = newCityData.getMatrixTravel().getTravelMatrix();
        carrierMatrix = newCityData.getMatrixTravel().getCarrierMatrix();
    }

    // This is based on Yen's algorithm:
    // http://en.wikipedia.org/wiki/Yen%27s_Algorithm
    public void findBestThree(String start, String end) {
        CityInfo startCity = new CityInfo();
        CityInfo endCity = new CityInfo();
        for (CityInfo city : cityInfo) {
            if (CityInfo.CitySet.getCityOrdinal(start) == CityInfo.CitySet.getCityOrdinal(city.getCityName())) {
                startCity = city;
            }
            if (CityInfo.CitySet.getCityOrdinal(end) == CityInfo.CitySet.getCityOrdinal(city.getCityName())) {
                endCity = city;
            }
        }

        // Find best option using Djikstra
        DjikstraRow[][] answers = new DjikstraRow[3][cityInfo.length];
        answers[0] = djikstra(startCity, endCity);
        // Find other two options using Yen's
        yen(startCity, endCity, answers);
        printAnswer(answers, startCity, endCity);
    }

    // Implements Yen's algoithm to find the three best routes. The best route
    // is found using Djikstra's, and that is the input to this algorithm. It 
    // essentially moves down the best route (the wikipedia page calls it a spur node
    // we keep that terminology here), one city at a time, running djikstra's
    // from the spur node to the end city, and combining that result with the root path
    // (which is from the start city to the node before the spur node). This is 
    // added to s set of potential paths. When every node in the best path is
    // so treated (excepting for obvious reasons, the last node), the best of these
    // options is added as the second best option. Yen's is then repeated, using
    // the best and second best routes as input (the spur node scanning is now
    // done on the second best route)
    public void yen(CityInfo startCity, CityInfo endCity, DjikstraRow[][] answers){
        // Since the array of DjikstraRows can only be read backward, we need a
        // forward linking representation:
        ArrayList<CityInfo> bestPath = new ArrayList<CityInfo>();
        ArrayList<ArrayList<CityInfo>> paths = new ArrayList<ArrayList<CityInfo>>();
        paths.add(bestPath);
        ArrayList<DjikstraRow[]> possibleSolution = new ArrayList<DjikstraRow[]>();
        bestPath.add(endCity);
        while(answers[0][CityInfo.CitySet.getCityOrdinal(bestPath.get(0).getCityName())].getPredicessorNode() != null){
            bestPath.add(0, answers[0][CityInfo.CitySet.getCityOrdinal(bestPath.get(0).getCityName())].getPredicessorNode());
        }
        generatePossible(paths, possibleSolution, answers);
        // If possible solution is empty, there are is only one legitimate path
        // as is the case of HYD to CPT, DXB to HYD, or DXB to CPT (possibly others)
        if(possibleSolution.isEmpty()){
            answers[1] = answers[0];
            answers[2] = answers[0];
        }
        else{
            answers[1] = getBest(possibleSolution, endCity);
            ArrayList<CityInfo> secondPath = new ArrayList<CityInfo>();
            secondPath.add(endCity);
            while(answers[1][CityInfo.CitySet.getCityOrdinal(secondPath.get(0).getCityName())].getPredicessorNode() != null){
                secondPath.add(0, answers[1][CityInfo.CitySet.getCityOrdinal(secondPath.get(0).getCityName())].getPredicessorNode());
            }
            paths.add(secondPath);
            generatePossible(paths, possibleSolution, answers);
            //In the case that the possibleSolution container is empty, then there are only tw0
            //distinct paths. I don't think this occurs with our dataset, but I am unsure
            // but, for example, _if_ Cape Town and Hyderabad had a connection between them, 
            //than DXB to CPT or DXB to HYD would have only two valid paths 
            if(possibleSolution.isEmpty()){
                answers[2] = answers[0];
            }
            else{
                    answers[2] = getBest(possibleSolution, endCity);
            }
        }
    }
    
    // Selects the best possible answer
    public DjikstraRow[] getBest(ArrayList<DjikstraRow[]> possibleSolution, CityInfo endCity){
        int endIndex = CityInfo.CitySet.getCityOrdinal(endCity.getCityName());
        DjikstraRow[] bestRow = possibleSolution.get(0);
        int bestIndex = 0;
        for(int i = 1; i < possibleSolution.size(); i++){
            if(possibleSolution.get(i)[endIndex].getCurrentCost() < bestRow[endIndex].getCurrentCost()){
                bestRow = possibleSolution.get(i);
                bestIndex = i;
            }
        }
        possibleSolution.remove(bestIndex);
        return bestRow;
    }
    
    // This is the meat of Yen's algorithm. This generates the spur node, root path,
    // spur path, combines them to form a route option, and adds that to the possible
    // answer container.
    public void generatePossible(ArrayList<ArrayList<CityInfo>> paths, ArrayList<DjikstraRow[]> possibleSolution, DjikstraRow[][] answers){
        ArrayList<CityInfo> currentPath = paths.get(paths.size() - 1);
        for(int i = 0; i < (currentPath.size() - 1); i++){
            CityInfo spurNode = currentPath.get(i);
            // create root path:
            DjikstraRow[] rootPath = new DjikstraRow[cityInfo.length];
            for (int j = 0; j < rootPath.length; j++) {
                rootPath[j] = new DjikstraRow();
            }
            for(int j = 0; j < i; j++){
                int cityIndex = CityInfo.CitySet.getCityOrdinal(currentPath.get(j).getCityName());
                rootPath[cityIndex] = answers[paths.size() - 1][cityIndex];
            }
            // create copy of cost matrix to modify
            float[][] adjustedMatrix = new float[costMatrix.length][costMatrix[0].length];
            for(int j = 0; j < adjustedMatrix.length; j++){
                for(int k = 0; k < adjustedMatrix[0].length; k++){
                    adjustedMatrix[j][k] = costMatrix[j][k];
                }
            }
            //remove all nodes except the last in rootPath from the new matrix
            for(int j = 0; j < i; j++){
                int cityIndex = CityInfo.CitySet.getCityOrdinal(currentPath.get(j).getCityName());
                for(int k = 0; k < adjustedMatrix.length; k++){
                    adjustedMatrix[cityIndex][k] = -1.0f;
                    adjustedMatrix[k][cityIndex] = -1.0f;
                }
            }
            //remove edges at 'spur' node that are in a solution
            for(int j = 0; j < paths.size(); j++){
                int spurIndex = CityInfo.CitySet.getCityOrdinal(spurNode.getCityName());
                for(int k = 0; k < paths.get(j).size(); k++){
                    if(spurIndex == CityInfo.CitySet.getCityOrdinal(paths.get(j).get(k).getCityName())){
                        adjustedMatrix[spurIndex][CityInfo.CitySet.getCityOrdinal(paths.get(j).get(k + 1).getCityName())] = -1.0f;
                    }
                }
                
            }
            //get cost at spurNode
            float spurCost = answers[paths.size() - 1][CityInfo.CitySet.getCityOrdinal(spurNode.getCityName())].getCurrentCost();
            //get partial 'spur path'
            DjikstraRow[] spurPath = djikstra(spurNode, currentPath.get(currentPath.size() - 1), adjustedMatrix, spurCost);
            
            // create entire Path (storing in rootPath)
            CityInfo end = spurPath[CityInfo.CitySet.getCityOrdinal(currentPath.get(currentPath.size() - 1).getCityName())].getNodeIdentity();
            while(end != null){
                // get current index in spurPath
                int endIndex = CityInfo.CitySet.getCityOrdinal(end.getCityName());
                //copy info to root path
                rootPath[endIndex].setNodeIdentity(spurPath[endIndex].getNodeIdentity());
                rootPath[endIndex].setCurrentCost(spurPath[endIndex].getCurrentCost());
                rootPath[endIndex].setPredicessorNode(spurPath[endIndex].getPredicessorNode());
                // update end
                end = spurPath[endIndex].getPredicessorNode();                
            }
            if(i != 0){
                rootPath[CityInfo.CitySet.getCityOrdinal(spurNode.getCityName())].setPredicessorNode(currentPath.get(i - 1));
            }
            else{
                rootPath[CityInfo.CitySet.getCityOrdinal(spurNode.getCityName())].setNodeIdentity(spurNode);
                rootPath[CityInfo.CitySet.getCityOrdinal(spurNode.getCityName())].setCurrentCost(0);
                rootPath[CityInfo.CitySet.getCityOrdinal(spurNode.getCityName())].setPredicessorNode(null);
            }
            // if is a full path, add to possibleSolution
            if(isComplete(rootPath, currentPath.get(0), currentPath.get(currentPath.size() - 1))){
                possibleSolution.add(rootPath);
            }
            
        }
    }
    
    // Checks to see if a possible solution connects from the start city to the
    // end city
    public boolean isComplete(DjikstraRow[] path, CityInfo start, CityInfo end){
        int startIndex = CityInfo.CitySet.getCityOrdinal(start.getCityName());
        int endIndex = CityInfo.CitySet.getCityOrdinal(end.getCityName());
        if(path[endIndex].getNodeIdentity() == null){
            return false;
        }
        else{
            boolean startToFinish = false;
            CityInfo endCondition = path[endIndex].getPredicessorNode();
            while(endCondition != null){
                int endConditionIndex = CityInfo.CitySet.getCityOrdinal(endCondition.getCityName());
                if(endConditionIndex == startIndex && path[endConditionIndex].getPredicessorNode() == null){
                    startToFinish = true;
                }
                endCondition = path[endConditionIndex].getPredicessorNode();
            }
            return startToFinish;
        }
    }
    
    // Djikstra's algorithm, our old friend
    public DjikstraRow[] djikstra(CityInfo start, CityInfo end) {
        String startCity = start.getCityName();
        int startCityCount = CityInfo.CitySet.getCityOrdinal(startCity);
        Queue<CostEdge> edgeList = new LinkedList<CostEdge>();

        DjikstraRow[] leastCost = new DjikstraRow[cityInfo.length];
        for (int i = 0; i < leastCost.length; i++) {
            leastCost[i] = new DjikstraRow();
        }
        leastCost[startCityCount].setCurrentCost(0.0f);
        leastCost[startCityCount].setNodeIdentity(start);
        leastCost[startCityCount].setPredicessorNode(null);

        //open initial cities:
        openCity(edgeList, start, cityInfo);
        //until all edges are processed:
        while (!edgeList.isEmpty()) {
            CostEdge currEdge = edgeList.poll();
            CityInfo predicessor = currEdge.getStartCity();
            CityInfo cityToUpdate = currEdge.getEndCity();
            int cityIndex = CityInfo.CitySet.getCityOrdinal(cityToUpdate.getCityName());
            float costSegment = currEdge.getCost();
            if (leastCost[cityIndex].getNodeIdentity() == null) {
                // If null, this is the first time this city has been updated, need 
                // to add this city's outgoing edges to the edgeList
                openCity(edgeList, cityToUpdate, cityInfo);
                leastCost[cityIndex].setNodeIdentity(cityToUpdate);
                leastCost[cityIndex].setPredicessorNode(predicessor);
                leastCost[cityIndex].setCurrentCost(costSegment + getPredicessorCost(predicessor, leastCost));
            } else {
                float totalCost = costSegment + getPredicessorCost(predicessor, leastCost);
                if (totalCost < leastCost[cityIndex].getCurrentCost()) {
                    leastCost[cityIndex].setPredicessorNode(predicessor);
                    leastCost[cityIndex].setCurrentCost(totalCost);
                }
            }
        }
        return leastCost;
    }

    // Over loaded Djikstra's that uses the altered adjacency matrix that is part
    // of Yen's. It also keeps track of the cost of the spur node (I wasn't doing
    // that initially, and it caused _lots_ of problems. :)
    public DjikstraRow[] djikstra(CityInfo start, CityInfo end, float[][] adjustedMatrix, float startCost) {
        String startCity = start.getCityName();
        int startCityCount = CityInfo.CitySet.getCityOrdinal(startCity);
        Queue<CostEdge> edgeList = new LinkedList<CostEdge>();

        DjikstraRow[] leastCost = new DjikstraRow[cityInfo.length];
        for (int i = 0; i < leastCost.length; i++) {
            leastCost[i] = new DjikstraRow();
        }
        leastCost[startCityCount].setCurrentCost(startCost);
        leastCost[startCityCount].setNodeIdentity(start);
        leastCost[startCityCount].setPredicessorNode(null);

        //open initial cities:
        openCity(edgeList, start, cityInfo, adjustedMatrix);
        //until all edges are processed:
        while (!edgeList.isEmpty()) {
            CostEdge currEdge = edgeList.poll();
            CityInfo predicessor = currEdge.getStartCity();
            CityInfo cityToUpdate = currEdge.getEndCity();
            int cityIndex = CityInfo.CitySet.getCityOrdinal(cityToUpdate.getCityName());
            float costSegment = currEdge.getCost();
            if (leastCost[cityIndex].getNodeIdentity() == null) {
                // If null, this is the first time this city has been updated, need 
                // to add this city's outgoing edges to the edgeList
                openCity(edgeList, cityToUpdate, cityInfo, adjustedMatrix);
                leastCost[cityIndex].setNodeIdentity(cityToUpdate);
                leastCost[cityIndex].setPredicessorNode(predicessor);
                leastCost[cityIndex].setCurrentCost(costSegment + getPredicessorCost(predicessor, leastCost));
            } else {
                float totalCost = costSegment + getPredicessorCost(predicessor, leastCost);
                if (totalCost < leastCost[cityIndex].getCurrentCost()) {
                    leastCost[cityIndex].setPredicessorNode(predicessor);
                    leastCost[cityIndex].setCurrentCost(totalCost);
                }
            }
        }
        return leastCost;
    }
    
    // Helper method for Djikstra's
    public void openCity(Queue<CostEdge> edgeList, CityInfo startCity, CityInfo[] cityInfo) {
        for (CityInfo connectedCity : cityInfo) {
            int startIndex = CityInfo.CitySet.getCityOrdinal(startCity.getCityName());
            int endIndex = CityInfo.CitySet.getCityOrdinal(connectedCity.getCityName());
            if (startIndex != endIndex && costMatrix[startIndex][endIndex] >= 0) {
                CostEdge newEdge = new CostEdge();
                newEdge.setStartCity(startCity);
                newEdge.setEndCity(connectedCity);
                newEdge.setCost(costMatrix[startIndex][endIndex]);
                newEdge.setCarrier(carrierMatrix[startIndex][endIndex]);
                edgeList.add(newEdge);
            }
        }

    }
    
    // Overloaded method to use the adjusted adjacency matrix for Yen's
    public void openCity(Queue<CostEdge> edgeList, CityInfo startCity, CityInfo[] cityInfo, float[][] adjustedMatrix) {
        for (CityInfo connectedCity : cityInfo) {
            int startIndex = CityInfo.CitySet.getCityOrdinal(startCity.getCityName());
            int endIndex = CityInfo.CitySet.getCityOrdinal(connectedCity.getCityName());
            if (startIndex != endIndex && adjustedMatrix[startIndex][endIndex] >= 0) {
                CostEdge newEdge = new CostEdge();
                newEdge.setStartCity(startCity);
                newEdge.setEndCity(connectedCity);
                newEdge.setCost(adjustedMatrix[startIndex][endIndex]);
                newEdge.setCarrier(carrierMatrix[startIndex][endIndex]);
                edgeList.add(newEdge);
            }
        }

    }

    // Prints the answer to the console
    public void printAnswer(DjikstraRow[][] answer, CityInfo startCity, CityInfo endCity){
        DecimalFormat df = new DecimalFormat("#,###,###,##0.00");
        StringBuilder output = new StringBuilder();
        int endIndex = CityInfo.CitySet.getCityOrdinal(endCity.getCityName());
        if(answer[0] == answer[1] && answer[0] == answer[2]){
            //case of only one unique path
            output.append("There is only one valid path from ");
            output.append(startCity.getCityName());
            output.append(" to ");
            output.append(endCity.getCityName());
            output.append("\nONLY option:\n\t");
            output.append("Cost: $");
            output.append(df.format(answer[0][endIndex].getCurrentCost()));
            output.append(" Route: ");
            getPath(output, endCity, answer[0]);
            output.append("(end) Carriers:");
            getCarrier(output, endCity, answer[0]);
            output.append("(end)\n");
            
            System.out.println(output.toString());
        }
        else if(answer[1] == answer[2]){
            //case of only two valid paths
            output.append("We only found two valid options from ");
            output.append(startCity.getCityName());
            output.append(" to ");
            output.append(endCity.getCityName());
            output.append("\nOption 1:\n\t");
            output.append("Cost: $");
            output.append(df.format(answer[0][endIndex].getCurrentCost()));
            output.append(" Route: ");
            getPath(output, endCity, answer[0]);
            output.append("(end) Carriers:");
            getCarrier(output, endCity, answer[0]);
            output.append("(end)\n");
            
            output.append("Option 2:\n\t");
            output.append("Cost: $");
            output.append(df.format(answer[1][endIndex].getCurrentCost()));
            output.append(" Route: ");
            getPath(output, endCity, answer[1]);
            output.append("(end) Carriers:");
            getCarrier(output, endCity, answer[1]);
            output.append("(end)\n");
            
            System.out.println(output.toString());
        }
        else{
            output.append("We found the best three options from ");
            output.append(startCity.getCityName());
            output.append(" to ");
            output.append(endCity.getCityName());
            output.append("\nOption 1:\n\t");
            output.append("Cost: $");
            output.append(df.format(answer[0][endIndex].getCurrentCost()));
            output.append(" Route: ");
            getPath(output, endCity, answer[0]);
            output.append("(end) Carriers:");
            getCarrier(output, endCity, answer[0]);
            output.append("(end)\n");
        
            output.append("Option 2:\n\t");
            output.append("Cost: $");
            output.append(df.format(answer[1][endIndex].getCurrentCost()));
            output.append(" Route: ");
            getPath(output, endCity, answer[1]);
            output.append("(end) Carriers:");
            getCarrier(output, endCity, answer[1]);
            output.append("(end)\n");
        
            output.append("Option 3:\n\t");
            output.append("Cost: $");
            output.append(df.format(answer[2][endIndex].getCurrentCost()));
            output.append(" Route: ");
            getPath(output, endCity, answer[2]);
            output.append("(end) Carriers:");
            getCarrier(output, endCity, answer[2]);
            output.append("(end)\n");
        
            System.out.println(output.toString());
        }
    }
    
    //Helper method for printing the path a flight takes
    public void getPath(StringBuilder output, CityInfo endCity, DjikstraRow[] leastCost){
            int endIndex = CityInfo.CitySet.getCityOrdinal(endCity.getCityName());
            if(leastCost[endIndex].getPredicessorNode() == null){
                output.append(leastCost[endIndex].getNodeIdentity().getCityName());
                output.append(" ->");
            }
            else{
                getPath(output, leastCost[endIndex].getPredicessorNode(), leastCost);
                output.append(leastCost[endIndex].getNodeIdentity().getCityName());
                output.append(" ->");
            }
        }
    
    //Helper method for printing the carriers for the given flights
    public void getCarrier(StringBuilder output, CityInfo endCity, DjikstraRow[] leastCost){
        int endIndex = CityInfo.CitySet.getCityOrdinal(endCity.getCityName());
        if(leastCost[endIndex].getPredicessorNode() == null){
                
        }
        else{
            getCarrier(output, leastCost[endIndex].getPredicessorNode(), leastCost);
            int predIndex = CityInfo.CitySet.getCityOrdinal(leastCost[endIndex].getPredicessorNode().getCityName());
            output.append(carrierMatrix[predIndex][endIndex]);
            output.append("->");
        }
    }
    
    // Get's the predicessor's cost
    public float getPredicessorCost(CityInfo predicessor, DjikstraRow[] leastCost) {
        int predIndex = CityInfo.CitySet.getCityOrdinal(predicessor.getCityName());
        return leastCost[predIndex].getCurrentCost();
    }

    /**
     * @return the cityInfo
     */
    public CityInfo[] getCityInfo() {
        return cityInfo;
    }

    /**
     * @param cityInfo the cityInfo to set
     */
    public void setCityInfo(CityInfo[] cityInfo) {
        this.cityInfo = cityInfo;
    }

    /**
     * @return the costMatrix
     */
    public float[][] getCostMatrix() {
        return costMatrix;
    }

    /**
     * @param costMatrix the costMatrix to set
     */
    public void setCostMatrix(float[][] costMatrix) {
        this.costMatrix = costMatrix;
    }

    /**
     * @return the carrierMatrix
     */
    public String[][] getCarrierMatrix() {
        return carrierMatrix;
    }

    /**
     * @param carrierMatrix the carrierMatrix to set
     */
    public void setCarrierMatrix(String[][] carrierMatrix) {
        this.carrierMatrix = carrierMatrix;
    }
}
