package ru.zzzzzzerg.linden.iap;

#if android

import haxe.Json;
import ru.zzzzzzerg.linden.GoogleIAP;

class PurchaseHandler
{
  public var item : PurchaseInfo;

  public function new()
  {
    item = null;
  }

  public function purchased(jsonString : String)
  {
    item = Json.parse(jsonString);
  }

  public function cancelled(response : Int)
  {
  }

  public function finished()
  {
  }
}

#end
