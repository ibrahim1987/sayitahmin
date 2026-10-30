package com.hmmi.guessnumber;

public class DuelPlayerModel {

    public String uid = "";
    public String fullname = "";
    public String email = "";

    public String deviceId = "";
    public String lastIp = "";
    public String kullaniciadi = "";
    public String username = "";
    public long puan = 0;
    public long duelPoint = 0;
    public long monthlyDuelPoint = 0;
    public String monthlyDuelMonthKey = "";
    public long updatedAt = 0;

    public int toplamDuelo = 0;
    public int kazandigiDuelo = 0;

    public int rankNo = 0;
    public long calculatedWinPoint = 1000;

    public DuelPlayerModel() {
    }
}
