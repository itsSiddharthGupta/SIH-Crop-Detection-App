package com.kars.sihpoc;

public class CropDetails {
    private String growthTime;
    private String marketRate;
    private String rotationCrops;
    private String yield;
    private String soilType;

    public CropDetails() {
    }

    public CropDetails(String growthTime, String marketRate, String rotationCrops, String yield, String soilType) {
        this.growthTime = growthTime;
        this.marketRate = marketRate;
        this.rotationCrops = rotationCrops;
        this.yield = yield;
        this.soilType = soilType;
    }

    public String getGrowthTime() {
        return growthTime;
    }

    public void setGrowthTime(String growthTime) {
        this.growthTime = growthTime;
    }

    public String getMarketRate() {
        return marketRate;
    }

    public void setMarketRate(String marketRate) {
        this.marketRate = marketRate;
    }

    public String getRotationCrops() {
        return rotationCrops;
    }

    public void setRotationCrops(String rotationCrops) {
        this.rotationCrops = rotationCrops;
    }

    public String getYield() {
        return yield;
    }

    public void setYield(String yield) {
        this.yield = yield;
    }

    public String getSoilType() {
        return soilType;
    }

    public void setSoilType(String soilType) {
        this.soilType = soilType;
    }

    @Override
    public String toString() {
        return "CropDetails{" +
                "growthTime='" + growthTime + '\'' +
                ", marketRate='" + marketRate + '\'' +
                ", rotationCrops='" + rotationCrops + '\'' +
                ", yield='" + yield + '\'' +
                ", soilType='" + soilType + '\'' +
                '}';
    }
}
