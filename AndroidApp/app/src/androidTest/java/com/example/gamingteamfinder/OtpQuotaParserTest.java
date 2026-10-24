
package com.example.gamingteamfinder;

import static org.junit.Assert.assertArrayEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OtpQuotaParserTest {

    // TEST 1: Valid quota received from backend
    @Test
    public void validQuotaReturnsSevenOfTen() throws Exception {

        JSONObject response = new JSONObject();
        response.put("verified", true);
        response.put("remaining", 7);
        response.put("maxRequests", 10);

        int[] quota =
                OtpApiClient.readVerifiedOtpQuota(response);

        assertArrayEquals(new int[]{7, 10}, quota);
    }

    // TEST 2: Zero remaining requests
    @Test
    public void zeroRemainingIsValid() throws Exception {

        JSONObject response = new JSONObject();
        response.put("verified", true);
        response.put("remaining", 0);
        response.put("maxRequests", 10);

        int[] quota =
                OtpApiClient.readVerifiedOtpQuota(response);

        assertArrayEquals(new int[]{0, 10}, quota);
    }

    // TEST 3: Backend does not return quota
    @Test
    public void missingQuotaReturnsUnknown() throws Exception {

        JSONObject response = new JSONObject();
        response.put("verified", true);

        int[] quota =
                OtpApiClient.readVerifiedOtpQuota(response);

        assertArrayEquals(new int[]{-1, -1}, quota);
    }

    // TEST 4: Verification failed
    @Test
    public void unverifiedResponseReturnsUnknown() throws Exception {

        JSONObject response = new JSONObject();
        response.put("verified", false);
        response.put("remaining", 7);
        response.put("maxRequests", 10);

        int[] quota =
                OtpApiClient.readVerifiedOtpQuota(response);

        assertArrayEquals(new int[]{-1, -1}, quota);
    }
}
