<!--
Copyright (C) 2020-2021 National Institute of Informatics

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# SINETStream for Android

## 概要

本ライブラリは、Android端末上のユーザアプリケーションが収集したデータを
[SINETStream](https://nii-gakunin-cloud.github.io/sinetstream)
経由で中継サーバ (ブローカ) に送信したり、
そこから受信したデータをユーザ
アプリケーションに渡すといった機能を提供するものである。

```
      #----------------------------------+
      |        User Application          |
      +----------------------------------+
                  |        A
    ==============|========|================ SINETStream API
          methods |        | callbacks
                  V        |
      +----------------------------------+
      |///  SINETStream for Android   ///| <--------+
      +----------------------------------+          |
             +-------------------+    A             |
             | Paho MQTT Android |    |             |
             +-------------------+    | [SSL/TLS]   | [config file]
                  |        A          |             |
    ==============|========|==========|=============|======= Android System
                  V        |          |             |
               +--------------+  +----------+  +----------+
               |    Network   |  | KeyChain |  | File I/O |
               +--------------+  +----------+  +----------+
                  |        A
    ==============|========|================================ Hardware
                  V        |
             [   MQTT Message   ]
```

Java版やPython版とは異なり、Android版のSINETStreamライブラリは
足回りのメッセージングシステムとして(現状では)
[Paho MQTT Android](https://www.eclipse.org/paho/index.php?page=clients/android/index.php)
のみを利用する。

SINETStreamの動作内容は、ライブラリ初期化時に規定の設定ファイル
（/data/data/__PACKAGE__/files/sinetstream_config.yml）
を読み込むことで決定される。
すなわち、本ライブラリはユーザアプリケーションが事前にこの設定
ファイルを用意していることを前提とする。

## Androidシステム認証ストレージの利用

本ライブラリv1.6よりSSL/TLS証明書の扱いを変更する。
対向ブローカとのSSL/TLS接続時にクライアント証明書や自己署名サーバ
証明書（いわゆるオレオレサーバ証明書）が求められる運用があり得る。

従来は、ユーザアプリケーションのソースコードを開発環境で開き、
アセット領域に所用のSSL/TLS証明書ファイルを埋め込みビルドしたAPK
を用意していた。
```
$(TOP)
  +-- app/
       +-- src/
            +-- main/
                 +-- assets/    <-- SSL/TLS証明書ファイル
                 +-- res/
                      +-- raw/  <-- SINETStream設定ファイル
```
しかし、これでは証明書を更新するたびにアプリケーションの再構築が
必要となり使い勝手が悪い。悪意の第三者によるアプリケーション解析
により秘匿情報が漏洩する危険性もある。

本ライブラリv1.6からは、キーストア[1]と呼ばれるシステム秘匿領域に
認証情報（ここではSSL/TLS証明書）を手動で導入しておき、ユーザ
アプリケーション実行時にこれらを動的に参照する方式とする。

[1] Android Keystore システム
https://developer.android.com/training/articles/keystore?hl=ja

## データ暗号化対応

対向ブローカとの通信路を秘匿するためにSSL/TLSを使うとして、データ
そのものを暗号化したい利用要件があり得る。
SINETStream設定ファイルの記述により、`Writer`側での暗号化、および
`Reader`側での復号処理を有効化できるようにした。

利用要件に合わせ、以下の各項目を適切に設定すること。

* 暗号化処理の利用
    * アルゴリズム
    * 鍵長
    * モード
    * パディング
    * パスワード
    * 鍵導出関数の利用
        * アルゴリズム
        * ソルト
        * 繰り返し数

## 本ライブラリの利用方法

### ビルド環境設定：参照先リポジトリおよび依存関係

ユーザアプリケーション開発者は、自身のビルド制御ファイル
「$(TOP)/app/build.gradle」
に以下の内容を追記すること。

```build.gradle
//
// We use the Eclipse Paho Android Service
// https://github.com/eclipse/paho.mqtt.android#gradle
//
repositories {
    maven { url "https://niidp.pages.vcp-handson.org/sinetstream-android/" }
    maven { url "https://repo.eclipse.org/content/repositories/paho-snapshots/" }
}

dependencies {
    implementation 'jp.ad.sinet.stream.android:sinetstream-android:1.6.0'

    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
    implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
}
```

### ビルド環境設定：SDKバージョン

Android動作環境の制約（API 26未満では動かない）

「SINETStream for Android」は、メッセージのシリアライザ／デシリアライザ機能で
「Apache Avro」ライブラリを利用している。
このAvroの実装内容[1]が制約条件となり、本ライブラリ自身のみならず本ライブラリ
を用いるユーザアプリケーションはすべて
「API 26 (Android 8.0)」以降でないとビルドエラー[2]となる。

```build.gradle
defaultConfig {
    targetSdkVersion: (現時点の最新)
    minSdkVersion: 26
    ...
}
```

[1] [Avro 1.9.2 Java library does not work on Android](https://www.mail-archive.com/dev@avro.apache.org/msg24138.html)

[2] build.gradleの「minSdkVersion」が26より小さい場合のビルドエラー
```
MethodHandle.invoke and MethodHandle.invokeExact are only supported starting with Android O (--min-api 26)
```

### ビルド環境設定：マニフェストファイル

Android版のSINETStreamが利用する「Paho MQTT Android」では、
ユーザインタフェースを扱う
[MqttAndroidClient](https://www.eclipse.org/paho/files/android-javadoc/org/eclipse/paho/android/service/MqttAndroidClient.html)
、およびバックグラウンドで通信を担う
[MqttService](https://www.eclipse.org/paho/files/android-javadoc/org/eclipse/paho/android/service/MqttService.html)
から構成される。

概要欄で示したようなモジュール階層間の依存関係の連鎖を解決するため、
Android版のSINETStreamライブラリを用いるユーザアプリケーションは
マニフェストファイルに
[MqttService](https://www.eclipse.org/paho/files/android-javadoc/org/eclipse/paho/android/service/MqttService.html)
の使用を宣言する必要がある。

この宣言を忘れると、対向ブローカと通信できない。
```xml
<manifest>
    <application>
        <!-- Paho MQTT Android 内部で利用するMQTTサービス -->
        <service android:name="org.eclipse.paho.android.service.MqttService" />
    </application>
</manifest>
```

また、インターネットアクセス用、およびAndroidの認証ストレージ利用に
必要となる権限も併せて記述する。

```xml
<manifest>
    <!-- インターネットアクセス用 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Androidの認証ストレージ利用 -->
    <uses-permission android:name="android.permission.USE_CREDENTIALS" />
</manifest>
```


### SINETStreamの設定ファイルの配置

概要欄で述べたように、
アプリケーション固有のデータ領域にSINETStreamの設定ファイルを配置する。
その記述例を以下に示す。

```yaml
service-1:
  type: mqtt
  brokers: mqtt.vcp-handson.org
  tls: true
  transport: websocket
  username_pw_set:
    username: mqtt
    password: TlQr6657bEWP1zGp
  topic: test-sinetstream-android-20191021
  consistency: AT_LEAST_ONCE
  retain: true
  client_id: client-sinetstream-android-20191021-1
  connect:
    keepalive: 30
    automatic_reconnect: true
```
