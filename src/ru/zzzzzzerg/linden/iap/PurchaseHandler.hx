package ru.zzzzzzerg.linden.iap;

import haxe.Json;

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

