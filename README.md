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
      |     SINETStream for Android      |---[ config file ]
      +----------------------------------+
      +----------------------------------+
      |        Paho MQTT Android         |
      +----------------------------------+
                  |        A
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


## 本ライブラリの利用方法

### ビルド環境設定：参照先リポジトリおよび依存関係

ユーザアプリケーション開発者は、自身のビルド制御ファイル
「$(TOP)/app/build.gradle」
に以下の内容を追記すること。

```
//
// We use the Eclipse Paho Android Service
// https://github.com/eclipse/paho.mqtt.android#gradle
//
repositories {
    maven { url "https://niidp.pages.vcp-handson.org/sinetstream-android/" }
    maven { url "https://repo.eclipse.org/content/repositories/paho-snapshots/" }
}

dependencies {
    implementation 'jp.ad.sinet.stream.android:sinetstream-android:1.5.2'

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
```
defaultConfig {
    targetSdkVersion: (現時点の最新)
    minSdkVersion: 26
    ...
}
```

[1] Avro 1.9.2 Java library does not work on Android
https://www.mail-archive.com/dev@avro.apache.org/msg24138.html

[2] build.gradleの「minSdkVersion」が26より小さい場合のビルドエラー
`
MethodHandle.invoke and MethodHandle.invokeExact are only supported starting with Android O (--min-api 26)
` 

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


また、インターネットに接続するAndroidアプリケーションの常として、所用の
権限も併せて記述する。

```
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
(中略)
        </activity>

        <service android:name="org.eclipse.paho.android.service.MqttService" />
    </application>
```


### SINETStreamの設定ファイルの配置

アプリケーションのローカルストレージにSINETStreamの設定ファイルを配置する。
（/data/data/__PACKAGE__/files/sinetstream_config.yml）

設定ファイルの記述内容の例を以下に示す。
```
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
