package ru.zzzzzzerg.linden;

#if android

import openfl.utils.JNI;
import haxe.Json;

import ru.zzzzzzerg.linden.iap.ConnectionHandler;
import ru.zzzzzzerg.linden.iap.PurchaseHandler;

typedef ProductInfo = {
  title : String,
  price : String,
  type : String,
  description : String,
  productId : String,
};

typedef PurchaseInfo = {
  developerPayload : String,
  orderId : String,
  productId : String,
  purchaseToken : String,
  purchaseTime : String,
  purchaseState : String,
  packageName : String,
};

class GoogleIAPImpl
{
  private static var _requestCode = 10000;

  public function new(licensePublicKey : String, handler : ConnectionHandler)
  {
    initJNI();

    _createService(licensePublicKey, handler);
  }

  public function createService(licensePublicKey : String, handler : ConnectionHandler)
  {
    _createService(licensePublicKey, handler);
  }

  public function destroyService()
  {
    _destroyService();
  }

  public function isServiceReady() : Bool
  {
    return _isServiceReady();
  }

  public function getItems(items : Array<String>) : Array<ProductInfo>
  {
    var productsInfo : Array<ProductInfo> = Lambda.array(Lambda.map(_getItems(items), Json.parse));
    return productsInfo;
  }

  public function getPurchases() : Array<PurchaseInfo>
  {
    var purchasesInfo : Array<PurchaseInfo> = Lambda.array(Lambda.map(_getPurchases(), Json.parse));
    return purchasesInfo;
  }

  public function consumeItem(productId : String, purchaseToken : String) : Bool
  {
    return _consumeItem(productId, purchaseToken);
  }

  public function purchaseItem(productId : String, handler : PurchaseHandler) : Bool
  {
    _requestCode += 1;
    return _purchaseItem(productId, _requestCode, handler);
  }

  private static function initJNI()
  {
    if(_createService == null)
    {
      _createService = JNI.createStaticMethod("ru/zzzzzzerg/linden/GoogleIAP",
          "createService", "(Ljava/lang/String;Lorg/haxe/lime/HaxeObject;)V");
    }

    if(_destroyService == null)
    {
      _destroyService = JNI.createStaticMethod("ru/zzzzzzerg/linden/GoogleIAP",
          "destroyService", "()V");
    }

    if(_isServiceReady == null)
    {
      _isServiceReady = JNI.createStaticMethod("ru/zzzzzzerg/linden/GoogleIAP",
          "isServiceReady", "()Z");
    }

    if(_getItems == null)
    {
      _getItems = JNI.createStaticMethod("ru/zzzzzzerg/linden/GoogleIAP",
          "getItems", "([Ljava/lang/String;)[Ljava/lang/String;");
    }

    if(_getPurchases == null)
    {
      _getPurchases = JNI.createStaticMethod("ru/zzzzzzerg/linden/GoogleIAP",
          "getPurchases", "()[Ljava/lang/String;");
    }

    if(_consumeItem == null)
    {
      _consumeItem = JNI.createStaticMethod("ru/zzzzzzerg/linden/GoogleIAP",
          "consumeItem", "(Ljava/lang/String;Ljava/lang/String;)Z");
    }

    if(_purchaseItem == null)
    {
      _purchaseItem = JNI.createStaticMethod("ru/zzzzzzerg/linden/GoogleIAP",
          "purchaseItem", "(Ljava/lang/String;ILorg/haxe/lime/HaxeObject;)Z");
    }
  }

  private static var _createService : Dynamic = null;
  private static var _destroyService : Dynamic = null;
  private static var _isServiceReady : Dynamic = null;
  private static var _getItems : Dynamic = null;
  private static var _getPurchases : Dynamic = null;
  private static var _consumeItem : Dynamic = null;
  private static var _purchaseItem : Dynamic = null;

}

typedef GoogleIAP = GoogleIAPImpl;

#else

class GoogleIAPFallback
{
  public function new(licensePublicKey : String, handler : ConnectionHandler)
  {
    handler.onServiceCreated(false);
  }

  public function createService(licensePublicKey : String, handler : ConnectionHandler)
  {
    handler.onServiceCreated(false);
  }

  public function destroyService()
  {
  }

  public function isServiceReady() : Bool
  {
    return false;
  }

  public function getItems(items : Array<String>) : Array<ProductInfo>
  {
    return [];
  }

  public function getPurchases() : Array<PurchaseInfo>
  {
    return [];
  }

  public function consumeItem(productId : String, purchaseToken : String) : Bool
  {
    return false;
  }

  public function purchaseItem(productId : String, handler : PurchaseHandler) : Bool
  {
    return false;
  }
}

typedef GoogleIAP = GoogleIAPFallback;

#end
