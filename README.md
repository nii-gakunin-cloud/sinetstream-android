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

Java版やPython版とは異なり、Android版のSINETStreamライブラリは
足回りのメッセージングシステムとして(現状では)
[Paho MQTT Android](https://www.eclipse.org/paho/index.php?page=clients/android/index.php)
のみを利用する。

## SINETStream設定方法

本ライブラリの設定方法として、以下の2通りを用意する。

1）[規定場所に事前に配置された設定ファイルを読み込む](#設定ファイル利用による静的指定)

2）[設定サーバ(コンフィグサーバ)から設定内容を動的にダウンロードする](#設定サーバ(コンフィグサーバ)の利用)

### 設定ファイル利用による静的指定
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

本方式による設定の場合、アプリケーション固有のデータ領域にYAML形式の
設定ファイル

`（/data/data/__PACKAGE__/files/sinetstream_config.yml）`

を事前に用意しておく。ファイル名は固定である。
本ライブラリ初期化時に上記設定ファイルが読み込まれ、動作内容が決定される。
書式など詳細は
[Android版のSINETStream設定ファイル](https://www.sinetstream.net/docs/userguide/config-android.html)
を参照されたい。

なお、対向MQTTブローカとの接続にSSL/TLSを用いる設定の場合、後述の
[Androidシステム認証ストレージの利用](#Androidシステム認証ストレージの利用)
を参照のうえで、関連の証明書類も事前に手動で導入しておくこと。

### 設定サーバ(コンフィグサーバ)の利用

```
      #----------------------------------+          +-----------------+
      |        User Application          |          | Web Browser     |
      +----------------------------------+          +-----------------+
                  |        A                                A
    ==============|========|================ API            | [HTTPS]
          methods |        | callbacks                      |
                  V        |                                V
      +----------------------------------+          +-----------------+
      |SINETStream for Android           |          | Config Server   |
      |                +---------------+ |          |                 |
      |                | Config Client | <--+       |      [Database] |
      |                +---------------+ |  |       |           A     |
      +-------------------------A--------+  |       +-----------|-----+
        +-------------------+   |           |                   |
        | Paho MQTT Android |   | [HTTPS]   | [AccessToken]     |
        +-------------------+   |           |                   |
                  A             |           |                   |
    ==============|=============|===========|== Android         |
                  V             V           |                   |
           +-----------------------+  +----------+              |
           |    Network            |  | File I/O |              |
           +-----------------------+  +----------+              |
                  A             A                               |
    ==============|=============|=========== Hardware           |
                  |             |                               |
                  V             |                               |
          [ MQTT Message ]      +-----------[ HTTPS ]-----------+
```

本方式による設定の場合、SINETStream設定サーバ(
[コンフィグサーバ](http://manual.config-server.sinetstream.net/manual/docs/home/)
と呼称)と連携して運用する。
まずコンフィグサーバへの接続情報およびユーザ認証のため`アクセストークン`
が当該サーバから払い出される。これをAndroid端末のウェブブラウザ`Chrome`
などで事前にダウンロードしておく。失効した場合は新しいもので置き換える。

本ライブラリ初期化時に`アクセストークン`と共にコンフィグサーバに接続する。
システム管理者によりコンフィグサーバ側で所用の情報が設定されているはずなので
必要なものを選択してダウンロードする。このときSSL/TLS証明書など秘匿情報が
あればこれらも併せて自動的に取得される。

本方式による設定では、各Android端末の動作内容はコンフィグサーバから動的に
ダウンロードした設定情報で規定される。
すなわち、複数のAndroid端末で同一の設定内容を使用したり、特定用途の設定を
個別に用意したりと柔軟な運用が可能である。またシステムで用いるAndroid端末
群に関してコンフィグサーバ側で一元管理を図れることを意味する。

各Android端末上で管理すべき情報は`アクセストークン`のみである。
コンフィグサーバから動的に取得した設定情報をメモリ上でのみ扱うようにする
ことで、情報セキュリティを強化できることにも注意されたい。

## Androidシステム認証ストレージの利用

__本章は手元で用意した設定ファイルを読み込ませる方式で適用される。
コンフィグサーバを利用する場合は所用のSSl/TLS証明書が自動的に
ダウンロードされるため、本章は読み飛ばして構わない。__

対向ブローカとのSSL/TLS接続時に
[クライアント証明書](https://developer.android.com/training/articles/security-ssl?hl=ja#ClientCert)
や
[自己署名サーバ証明書](https://developer.android.com/training/articles/security-ssl?hl=ja#SelfSigned)
（いわゆるオレオレサーバ証明書）が求められる運用があり得る。

本ライブラリ実装では、Androidキーストア\[1\]と呼ばれるシステム秘匿領域に
目的のブローカとのSSL/TLS接続に必要な証明書を事前に手動で導入しておき、
ユーザアプリケーション実行時にこれらを参照する方式とする。
SSL/TLS証明書が失効した場合は、再発行されたものと入れ替える必要がある。

> \[1\]: [Android keystore システム](https://developer.android.com/training/articles/keystore?hl=ja)

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

```groovy
//
// We use the Eclipse Paho Android Service
// https://github.com/eclipse/paho.mqtt.android#gradle
//
repositories {
    maven { url "https://niidp.pages.vcp-handson.org/sinetstream-android/" }
    maven { url "https://repo.eclipse.org/content/repositories/paho-snapshots/" }
}

dependencies {
    implementation 'jp.ad.sinet.stream.android:sinetstream-android:x.y.z' // 最新版を指定する

    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
    implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
}
```

### ビルド環境設定：SDKバージョン

Android動作環境の制約（API 26未満では動かない）

「SINETStream for Android」は、メッセージのシリアライザ／デシリアライザ機能で
「Apache Avro」ライブラリを利用している。
このAvroの実装内容\[2\]が制約条件となり、本ライブラリ自身のみならず本ライブラリ
を用いるユーザアプリケーションはすべて
「API 26 (Android 8.0)」以降でないと以下のようなビルドエラー\[3\]となる。
```text
MethodHandle.invoke and MethodHandle.invokeExact are only supported starting with Android O (--min-api 26)
```

よって、プロジェクト全体のまたはモジュールごとの`build.gradle`の該当箇所で
動作可能なAndroid OSの下限を明記すること。
```groovy
android {
    defaultConfig {
        targetSdk: XX // 現時点の最新
        minSdk: 26  // 動作可能なAndroid OS（APIレベル）の下限
        // ...
    }
}
```

> \[2\]: [Avro 1.9.2 Java library does not work on Android](https://www.mail-archive.com/dev@avro.apache.org/msg24138.html)<br>
> \[3\]: [build.gradleの「minSdkVersion」が26より小さい場合のビルドエラー](https://issuetracker.google.com/issues/174733673)

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

また、インターネットアクセスに必要となる権限も併せて記述する。

```xml
<manifest>
    <!-- インターネットアクセス用 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
</manifest>
```

> <em>注意</em><br>
> Androidの認証ストレージを利用するための実行時権限 `USE_CREDENTIALS` は
> Android 6 (APIレベル23)で削除されたため、本ライブラリ使用に関しては指定不要。
> 詳細は
> [API23実行時権限差分](https://developer.android.com/sdk/api_diff/23/changes/android.Manifest.permission)
> を参照のこと。

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


## Paho Mqtt AndroidライブラリのAndroid 12以降への対応
### 背景
冒頭に記述したとおり、本ライブラリではMQTTメッセージングシステムの足回りとして
[Paho MQTT Android](https://www.eclipse.org/paho/index.php?page=clients/android/index.php)
を採用している。
Android 12におけるシステムAPIの挙動変更
[ペンディングインテントの可変性](https://developer.android.com/about/versions/12/behavior-changes-12?hl=ja#pending-intent-mutability)
（`PendingIntent`コンストラクタに渡す引数`flags`に設定すべき値が追加された）により、
既存の`Paho MQTT Android`実装だとブローカ接続直後に例外が発生するようになった。
本事象に対して修正パッチが何件か投稿されているが、肝心の本家のGitHubリポジトリ
[eclipse/paho.mqtt.android](https://github.com/eclipse/paho.mqtt.android)
が数年も放置されている状態となっている。

最終的に本家GitHubで対応されるまでの時限的な繋ぎとして、`Paho Mqtt Android`
ライブラリを手元で修正した版（`PahoMqttAndroid-Bugfix`）をリンクして使うことにする。

### アプリケーション側マニフェストファイルの追加修正

以下の利用権限を追加する。
```xml
<manifest>
    <!-- PahoMqttAndroid (Android 12+): ブローカ死活監視用 -->
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
</manifest>
```

### app/build.gradleの追加修正

本家の`org.eclipse.paho.android.service`の代わりに`pahomqttandroid-bugfix`を使う。
```groovy
//
// We use the Eclipse Paho Android Service
// https://github.com/eclipse/paho.mqtt.android#gradle
//
repositories {
    maven { url "https://niidp.pages.vcp-handson.org/sinetstream-android/" }
    maven { url "https://repo.eclipse.org/content/repositories/paho-snapshots/" }
}

dependencies {
    /* SINETStream-android */
    implementation 'jp.ad.sinet.stream.android:sinetstream-android:x.y.z' // 最新版を指定する

    /* Paho Mqtt */
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5' // 既存のものをそのまま使用
    // implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'  // コメントアウト
    implementation 'net.sinetstream:pahomqttandroid-bugfix:1.0.0'  // PahoMqttAndroid修正版を使用
}
```
