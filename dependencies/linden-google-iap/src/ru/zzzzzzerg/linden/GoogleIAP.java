package ru.zzzzzzerg.linden;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.util.Base64;
import android.text.TextUtils;
import android.content.IntentSender.SendIntentException;

import com.android.vending.billing.IInAppBillingService;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

import org.haxe.lime.HaxeObject;
import org.haxe.extension.Extension;

public class GoogleIAP extends Extension
{
  public GoogleIAP()
  {
    super();
    Log.d(tag, "Construct LindenGoogleIAP");
  }

  /**
   * Called when an activity you launched exits, giving you the requestCode
   * you started it with, the resultCode it returned, and any additional data
   * from it.
   */
  public boolean onActivityResult (int requestCode, int resultCode, Intent data)
  {
    handleActivityResult(requestCode, resultCode, data);
    return true;
  }


  /**
   * Called when the activity is starting.
   */
  public void onCreate(Bundle savedInstanceState)
  {
  }


  /**
   * Perform any final cleanup before an activity is destroyed.
   */
  public void onDestroy()
  {
    destroyService();
  }


  /**
   * Called as part of the activity lifecycle when an activity is going into
   * the background, but has not (yet) been killed.
   */
  public void onPause()
  {
  }


  /**
   * Called after {@link #onStop} when the current activity is being
   * re-displayed to the user (the user has navigated back to it).
   */
  public void onRestart()
  {
  }


  /**
   * Called after {@link #onRestart}, or {@link #onPause}, for your activity
   * to start interacting with the user.
   */
  public void onResume()
  {
  }


  /**
   * Called after {@link #onCreate} &mdash; or after {@link #onRestart} when
   * the activity had been stopped, but is now again being displayed to the
   * user.
   */
  public void onStart()
  {
  }


  /**
   * Called when the activity is no longer visible to the user, because
   * another activity has been resumed and is covering this one.
   */
  public void onStop()
  {
  }

  // Billing response codes
  public static final int BILLING_RESPONSE_RESULT_OK = 0;
  public static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
  public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
  public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;
  public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;
  public static final int BILLING_RESPONSE_RESULT_ERROR = 6;
  public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
  public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;

  // IAB Helper error codes
  public static final int IABHELPER_ERROR_BASE = -1000;
  public static final int IABHELPER_REMOTE_EXCEPTION = -1001;
  public static final int IABHELPER_BAD_RESPONSE = -1002;
  public static final int IABHELPER_VERIFICATION_FAILED = -1003;
  public static final int IABHELPER_SEND_INTENT_FAILED = -1004;
  public static final int IABHELPER_USER_CANCELLED = -1005;
  public static final int IABHELPER_UNKNOWN_PURCHASE_RESPONSE = -1006;
  public static final int IABHELPER_MISSING_TOKEN = -1007;
  public static final int IABHELPER_UNKNOWN_ERROR = -1008;
  public static final int IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE = -1009;
  public static final int IABHELPER_INVALID_CONSUMPTION = -1010;

  // Keys for the responses from InAppBillingService
  public static final String RESPONSE_CODE = "RESPONSE_CODE";
  public static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";
  public static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
  public static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
  public static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";
  public static final String RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
  public static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
  public static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
  public static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";

  // Item types
  public static final String ITEM_TYPE_INAPP = "inapp";
  public static final String ITEM_TYPE_SUBS = "subs";

  // some fields on the getSkuDetails response bundle
  public static final String GET_SKU_DETAILS_ITEM_LIST = "ITEM_ID_LIST";
  public static final String GET_SKU_DETAILS_ITEM_TYPE_LIST = "ITEM_TYPE_LIST";

  private static final String KEY_FACTORY_ALGORITHM = "RSA";
  private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

  private static String tag = "LindenGoogleIAP";
  private static String license = "";

  private static int billingVersion = 3;

  private static HashMap<Integer, String> responseDescriptions = new HashMap<Integer, String>();
  private static HashMap<Integer, HaxeObject> handlers = new HashMap<Integer, HaxeObject>();

  public static IInAppBillingService iapService = null;
  public static ServiceConnection iapServiceConnection = null;

  public static String packageName = "";

  public static void createService(String licensePublicKey, final HaxeObject callback)
  {
    if(iapService != null)
    {
      Log.w(tag, "Service already started");
    }

    initResponseDescriptions();

    iapService = null;
    iapServiceConnection = null;
    packageName = mainContext.getPackageName();
    license = licensePublicKey;

    iapServiceConnection = new ServiceConnection()
    {
      @Override
      public void onServiceDisconnected(ComponentName name)
      {
        iapService = null;
        Log.d(tag, "Billing service disconnected");
      }

      @Override
      public void onServiceConnected(ComponentName name, IBinder service)
      {
        iapService = IInAppBillingService.Stub.asInterface(service);
        Log.d(tag, "Billing service connected");

        try
        {
          int response = iapService.isBillingSupported(billingVersion, packageName,
              ITEM_TYPE_INAPP);
          if(response != BILLING_RESPONSE_RESULT_OK)
          {
            Log.i(tag, "Billing is not supported: " + response);
            iapService = null;
          }
          else
          {
            Log.i(tag, "Billing supported");
          }
        }
        catch(RemoteException e)
        {
          e.printStackTrace();

          iapService = null;
        }

        callbackHandler.post(new Runnable()
            {
              public void run()
              {
                callback.call("onServiceCreated", new Object[] { iapService != null, });
              }
            });
      }
    };

    Log.d(tag, "Create service intent");
    Intent serviceIntent = new Intent(
        "com.android.vending.billing.InAppBillingService.BIND");

    if(!mainContext.getPackageManager().queryIntentServices(serviceIntent, 0).isEmpty())
    {
      Log.d(tag, "Bind service");
      mainContext.bindService(serviceIntent,
          iapServiceConnection,
          Context.BIND_AUTO_CREATE);
    }
    else
    {
      Log.d(tag, "No service for intent");
    }
  }

  public static void destroyService()
  {
    if(iapServiceConnection != null)
    {
      Log.d(tag, "Unbind service");
      mainContext.unbindService(iapServiceConnection);
      iapServiceConnection = null;
      iapService = null;
      // FIXME: how to destroy iapService
    }
  }

  public static boolean isServiceReady()
  {
    return iapService != null;
  }

  public static String[] getItems(String[] items)
  {
    String[] itemsInfoList = new String[0];
    try
    {
      if(!isServiceReady())
      {
        Log.d(tag, "Service is not ready for getItems");
        return itemsInfoList;
      }

      Log.d(tag, "Getting items. Items list size: " + items.length);

      Log.d(tag, "Creating query bundle");
      Bundle query = new Bundle();
      ArrayList<String> skuList = new ArrayList<String>(Arrays.asList(items));
      query.putStringArrayList("ITEM_ID_LIST", skuList);

      Log.d(tag, "Getting sku details for package " + packageName);
      Bundle details = iapService.getSkuDetails(billingVersion, packageName,
          ITEM_TYPE_INAPP, query);
      if(!details.containsKey(RESPONSE_GET_SKU_DETAILS_LIST))
      {
        int response = getResponseCode(details);
        if(response != BILLING_RESPONSE_RESULT_OK)
        {
          Log.e(tag, getResponseDescription(response));
        }
        else
        {
          Log.w(tag, "No details for items");
        }
      }
      else
      {
        Log.d(tag, "Got sku details");
        ArrayList<String> res = details.getStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST);
        itemsInfoList = new String[res.size()];
        for(int idx = 0, cnt = res.size(); idx < cnt; ++idx)
        {
          itemsInfoList[idx] = res.get(idx);
        }
      }
    }
    catch(RemoteException e)
    {
      e.printStackTrace();
    }

    return itemsInfoList;
  }

  public static String[] getPurchases()
  {
    ArrayList<String> purchasesList = new ArrayList<String>();

    if(!isServiceReady())
    {
      Log.d(tag, "Service is not ready for getPurchases");
      return new String[0];
    }

    try
    {
      String continueToken = null;
      do
      {
        Bundle items = iapService.getPurchases(billingVersion, packageName,
            ITEM_TYPE_INAPP, continueToken);
        int response = getResponseCode(items);
        if(response != BILLING_RESPONSE_RESULT_OK)
        {
          Log.e(tag, getResponseDescription(response));
          break;
        }

        if(!items.containsKey(RESPONSE_INAPP_ITEM_LIST)
            || !items.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST)
            || !items.containsKey(RESPONSE_INAPP_SIGNATURE_LIST))
        {
          Log.w(tag, "Bundle doesn't contains required fields");
          break;
        }

        ArrayList<String> skus = items.getStringArrayList(RESPONSE_INAPP_ITEM_LIST);
        ArrayList<String> purchases = items.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST);
        ArrayList<String> signatures = items.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST);

        for(int i = 0, cnt = purchases.size(); i < cnt; ++i)
        {
          String data = purchases.get(i);
          String sku = skus.get(i);
          String signature = signatures.get(i);

          if(verify(license, data, signature))
          {
            purchasesList.add(data);
          }
          else
          {
            Log.w(tag, "Purchase signature verification failed for sku " + sku + " with index " + i);
          }
        }

        continueToken = items.getString(INAPP_CONTINUATION_TOKEN);
      }
      while(!TextUtils.isEmpty(continueToken));
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }

    String[] result = new String[purchasesList.size()];
    for(int idx = 0, cnt = purchasesList.size(); idx < cnt; ++idx)
    {
      result[idx] = purchasesList.get(idx);
    }
    return result;
  }

  public static boolean consumeItem(String sku, String token)
  {
    boolean consumed = false;
    try
    {
      if(!isServiceReady())
      {
        Log.d(tag, "Service is not ready for consumeItem");
      }
      else
      {
        int response = iapService.consumePurchase(billingVersion, packageName, token);
        if(response == BILLING_RESPONSE_RESULT_OK)
        {
          consumed = true;
        }
        else
        {
          Log.e(tag, getResponseDescription(response));
        }
      }
    }
    catch(RemoteException e)
    {
      e.printStackTrace();
    }

    return consumed;
  }

  public static boolean purchaseItem(String sku, int requestCode, HaxeObject callback)
  {
    try
    {
      if(!isServiceReady())
      {
        Log.d(tag, "Service is not ready for purchaseItem");
        return false;
      }

      Log.d(tag, "Getting BuyIntent for " + sku);
      Bundle bundle = iapService.getBuyIntent(billingVersion, packageName, sku,
          ITEM_TYPE_INAPP, "");
      int response = getResponseCode(bundle);
      if(response != BILLING_RESPONSE_RESULT_OK)
      {
        Log.e(tag, getResponseDescription(response));
        return false;
      }

      if(handlers.containsKey(requestCode))
      {
        Log.e(tag, "PurchaseItem with this code already exist: " + requestCode);
        return false;
      }

      handlers.put(requestCode, callback);

      PendingIntent intent = bundle.getParcelable(RESPONSE_BUY_INTENT);
      Log.d(tag, "Starting intent for " + sku + " with request code " + requestCode);

      mainActivity.startIntentSenderForResult(intent.getIntentSender(),
          requestCode, new Intent(),
          Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
      Log.d(tag, "Intent for " + sku + " with request code " +
          requestCode + "started");

      return true;
    }
    catch(Exception e)
    {
      e.printStackTrace();
      if(handlers.containsKey(requestCode))
      {
        Log.d(tag, "Exception occured for purchase with request code: " + requestCode);
        handlers.remove(requestCode);
      }
    }

    return false;
  }

  public static boolean handleActivityResult(final int requestCode, int resultCode, Intent data)
  {
    Log.d(tag, "handleActivityResult");
    if(!handlers.containsKey(requestCode))
    {
      Log.d(tag, "No handler for request code: " + requestCode);
      return false;
    }

    try
    {
      if(data == null)
      {
        Log.d(tag, "Null data for purchase activity result");
        return true;
      }

      final int response = getResponseCode(data.getExtras());
      if(resultCode == Activity.RESULT_OK && response == BILLING_RESPONSE_RESULT_OK)
      {
        final String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
        String signature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);

        if(purchaseData == null || signature == null)
        {
          String d = data.getExtras().toString();
          Log.w(tag, "Data or Signature is null: " + d);
        }
        else if(verify(license, purchaseData, signature))
        {
          callbackHandler.post(new Runnable()
              {
                public void run()
                {
                  HaxeObject callback = handlers.get(requestCode);
                  callback.call("purchased", new Object[] {purchaseData});
                }
              });
        }
      }
      else if(resultCode == Activity.RESULT_OK)
      {
        Log.e(tag, getResponseDescription(response));
      }
      else if(resultCode == Activity.RESULT_CANCELED)
      {
        callbackHandler.post(new Runnable()
            {
              public void run()
              {
                HaxeObject callback = handlers.get(requestCode);
                callback.call("cancelled", new Object[] {response});
              }
            });
      }
      else
      {
        Log.e(tag, "Unhandled result code: " + resultCode);
      }

      return true;
    }
    finally
    {
      callbackHandler.post(new Runnable()
          {
            public void run()
            {
              HaxeObject callback = handlers.remove(requestCode);
              callback.call("finished", new Object[]{});
            }
          });
    }
  }

  static boolean verify(String publicKey, String data, String signature)
  {
    if(!TextUtils.isEmpty(signature))
    {
      try
      {
        byte[] decodedKey = Base64.decode(publicKey, Base64.DEFAULT);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
        PublicKey key = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));

        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
        sig.initVerify(key);
        sig.update(data.getBytes());

        if(!sig.verify(Base64.decode(signature, Base64.DEFAULT)))
        {
          Log.w(tag, "Signature verification failed");
          return false;
        }
      }
      catch(Exception e)
      {
        e.printStackTrace();
        return false;
      }
    }

    return true;
  }

  // Workaround to bug where sometimes response codes come as Long instead of Integer
  static int getResponseCode(Bundle b)
  {
    Object o = b.get(RESPONSE_CODE);
    if (o == null)
    {
      Log.d(tag, "Bundle with null response code, assuming OK (known issue)");
      return BILLING_RESPONSE_RESULT_OK;
    }
    else if (o instanceof Integer)
    {
      return ((Integer)o).intValue();
    }
    else if (o instanceof Long)
    {
      return (int)((Long)o).longValue();
    }
    else
    {
      Log.d(tag, "Unexpected type for bundle response code.");
      Log.d(tag, o.getClass().getName());
      return BILLING_RESPONSE_RESULT_ERROR;
    }
  }

  static String getResponseDescription(int response)
  {
    String description = "Unknown";
    if(responseDescriptions.containsKey(response))
    {
      description = responseDescriptions.get(response);
    }

    return "" + response + ": " + description;
  }

  static void initResponseDescriptions()
  {
    responseDescriptions.put(BILLING_RESPONSE_RESULT_OK, "OK");
    responseDescriptions.put(BILLING_RESPONSE_RESULT_USER_CANCELED, "User cancel");
    responseDescriptions.put(2, "Unknown");
    responseDescriptions.put(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Billing Unavailable");
    responseDescriptions.put(BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE, "Item Unavailable");
    responseDescriptions.put(BILLING_RESPONSE_RESULT_DEVELOPER_ERROR, "Developer Error");
    responseDescriptions.put(BILLING_RESPONSE_RESULT_ERROR, "Error");
    responseDescriptions.put(BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED, "Item Already Owned");
    responseDescriptions.put(BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED, "Item Not Owned");

    responseDescriptions.put(IABHELPER_REMOTE_EXCEPTION, "Remote exception during initialization");
    responseDescriptions.put(IABHELPER_BAD_RESPONSE, "Bad response received");
    responseDescriptions.put(IABHELPER_VERIFICATION_FAILED, "Purchase signature verification failed");
    responseDescriptions.put(IABHELPER_SEND_INTENT_FAILED, "Send intent failed");
    responseDescriptions.put(IABHELPER_USER_CANCELLED, "User cancelled");
    responseDescriptions.put(IABHELPER_UNKNOWN_PURCHASE_RESPONSE, "Unknown purchase response");
    responseDescriptions.put(IABHELPER_MISSING_TOKEN, "Missing token");
    responseDescriptions.put(IABHELPER_UNKNOWN_ERROR, "Unknown error");
    responseDescriptions.put(IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE, "Subscriptions not available");
    responseDescriptions.put(IABHELPER_INVALID_CONSUMPTION, "Invalid consumption attempt");
  }


}
