/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Bosch Software Innovations - added Redis URL support with authentication
 *     Firis SA - added mDNS services registering 
 *******************************************************************************/
package org.eclipse.leshan.server.demo;

import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

//CF server dependencies:
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.x509.CertificateVerifier;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.cluster.RedisRegistrationStore;
import org.eclipse.leshan.server.cluster.RedisSecurityStore;
import org.eclipse.leshan.server.demo.servlet.ClientServlet;
import org.eclipse.leshan.server.demo.servlet.EventServlet;
import org.eclipse.leshan.server.demo.servlet.ObjectSpecServlet;
import org.eclipse.leshan.server.demo.servlet.SecurityServlet;
import org.eclipse.leshan.server.impl.FileSecurityStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StaticModelProvider;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

//mySQL dependencies.
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

//CF server dependencies:
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationService;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//default dependencies:
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.BindException;
import java.net.InetAddress;
import java.net.URI;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.cluster.RedisRegistrationStore;
import org.eclipse.leshan.server.cluster.RedisSecurityStore;
import org.eclipse.leshan.server.demo.servlet.ClientServlet;
import org.eclipse.leshan.server.demo.servlet.EventServlet;
import org.eclipse.leshan.server.demo.servlet.ObjectSpecServlet;
import org.eclipse.leshan.server.demo.servlet.SecurityServlet;
import org.eclipse.leshan.server.impl.FileSecurityStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StaticModelProvider;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

import org.eclipse.leshan.server.demo.*;
import org.eclipse.leshan.core.*;
import org.eclipse.leshan.server.observation.ObservationListener;

public class LeshanServerDemo {

    static {
        // Define a default logback.configurationFile
        String property = System.getProperty("logback.configurationFile");
        if (property == null) {
            System.setProperty("logback.configurationFile", "logback-config.xml");
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServerDemo.class);

    private final static String[] modelPaths = new String[] { "31024.xml",

                            "10241.xml", "10242.xml", "10243.xml", "10244.xml", "10245.xml", "10246.xml", "10247.xml",
                            "10248.xml", "10249.xml", "10250.xml",

                            "2048.xml", "2049.xml", "2050.xml", "2051.xml", "2052.xml", "2053.xml", "2054.xml",
                            "2055.xml", "2056.xml", "2057.xml",

                            "3200.xml", "3201.xml", "3202.xml", "3203.xml", "3300.xml", "3301.xml", "3302.xml",
                            "3303.xml", "3304.xml", "3305.xml", "3306.xml", "3308.xml", "3310.xml", "3311.xml",
                            "3312.xml", "3313.xml", "3314.xml", "3315.xml", "3316.xml", "3317.xml", "3318.xml",
                            "3319.xml", "3320.xml", "3321.xml", "3322.xml", "3323.xml", "3324.xml", "3325.xml",
                            "3326.xml", "3327.xml", "3328.xml", "3329.xml", "3330.xml", "3331.xml", "3332.xml",
                            "3333.xml", "3334.xml", "3335.xml", "3336.xml", "3337.xml", "3338.xml", "3339.xml",
                            "3340.xml", "3341.xml", "3342.xml", "3343.xml", "3344.xml", "3345.xml", "3346.xml",
                            "3347.xml", "3348.xml", "3349.xml", "3350.xml","32700.xml",

                            "Communication_Characteristics-V1_0.xml",

                            "LWM2M_Lock_and_Wipe-V1_0.xml", "LWM2M_Cellular_connectivity-v1_0.xml",
                            "LWM2M_APN_connection_profile-v1_0.xml", "LWM2M_WLAN_connectivity4-v1_0.xml",
                            "LWM2M_Bearer_selection-v1_0.xml", "LWM2M_Portfolio-v1_0.xml", "LWM2M_DevCapMgmt-v1_0.xml",
                            "LWM2M_Software_Component-v1_0.xml", "LWM2M_Software_Management-v1_0.xml",

                            "Non-Access_Stratum_NAS_configuration-V1_0.xml" };

    private final static String USAGE = "java -jar leshan-server-demo.jar [OPTION]\n\n";

    private final static String DEFAULT_KEYSTORE_TYPE = KeyStore.getDefaultType();

    private final static String DEFAULT_KEYSTORE_ALIAS = "leshan";

    private final static String DatabaseUrl = "jdbc:sqlite:/home/keyshav/ti/leshan/sqlite.db"; 

    public static void createNewDatabase() {
        try (Connection conn = DriverManager.getConnection(DatabaseUrl)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }
 
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
   
  public static void createNewTable() {

        String sql = "CREATE TABLE IF NOT EXISTS Table1 (\n"
                + "	parkspot_id integer PRIMARY KEY,\n"
        		+ " parkspot_status text, \n"
        		+ " vehicle_id integer, \n"
        		+ " vehicle_licenseplate text, \n"
        		+ " price_of_spot integer, \n"
        		+ " free_until DATETIME, \n"
        		+ " reserved_until DATETIME \n"
                + ");";
        try (Connection conn = DriverManager.getConnection(DatabaseUrl);
                Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
            System.out.println("created new table in database.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("Not able to create new table in database.");
        }
    }

    public static void sendColor(LeshanServer aLwServer, Registration aRegistration, String aColor) {
        try {
			WriteResponse writeresponse = aLwServer.send(aRegistration, new WriteRequest(3341,0,5527, aColor));
			if (writeresponse.isSuccess()) {
//			System.out.println("Device time:" + (writeresponse.toString()));
			}else {
			System.out.println("Failed to read:" + writeresponse.getCode() + " " + writeresponse.getErrorMessage());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    public static void sendOccupationStatus(LeshanServer aLwServer, Registration aRegistration, String aOccupationStatus) {
        try {
			WriteResponse writeresponse = aLwServer.send(aRegistration, new WriteRequest(32700,0,32801, aOccupationStatus));
			if (writeresponse.isSuccess()) {
//			System.out.println("Device time:" + (writeresponse.toString()));
			}else {
			System.out.println("Failed to read:" + writeresponse.getCode() + " " + writeresponse.getErrorMessage());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    public static void main(String[] args) {
        // Define options for command line tools
        Options options = new Options();

        final StringBuilder X509Chapter = new StringBuilder();
        X509Chapter.append("\n .");
        X509Chapter.append("\n .");
        X509Chapter.append("\n ===============================[ X509 ]=================================");
        X509Chapter.append("\n | By default Leshan demo uses an embedded self-signed certificate and  |");
        X509Chapter.append("\n | trusts any client certificates.                                      |");
        X509Chapter.append("\n | If you want to use your own server keys, certificates and truststore,|");
        X509Chapter.append("\n | you can provide a keystore using -ks, -ksp, -kst, -ksa, -ksap.       |");
        X509Chapter.append("\n | To get helps about files format and how to generate it, see :        |");
        X509Chapter.append("\n | See https://github.com/eclipse/leshan/wiki/Credential-files-format   |");
        X509Chapter.append("\n ------------------------------------------------------------------------");

        options.addOption("h", "help", false, "Display help information.");
        options.addOption("lh", "coaphost", true, "Set the local CoAP address.\n  Default: any local address.");
        options.addOption("lp", "coapport", true,
                String.format("Set the local CoAP port.\n  Default: %d.", LwM2m.DEFAULT_COAP_PORT));
        options.addOption("slh", "coapshost", true, "Set the secure local CoAP address.\nDefault: any local address.");
        options.addOption("slp", "coapsport", true,
                String.format("Set the secure local CoAP port.\nDefault: %d.", LwM2m.DEFAULT_COAP_SECURE_PORT));
        options.addOption("wh", "webhost", true, "Set the HTTP address for web server.\nDefault: any local address.");
        options.addOption("wp", "webport", true, "Set the HTTP port for web server.\nDefault: 8080.");
        options.addOption("m", "modelsfolder", true, "A folder which contains object models in OMA DDF(.xml) format.");
        options.addOption("r", "redis", true,
                "Set the location of the Redis database for running in cluster mode.\nThe URL is in the format of: 'redis://:password@hostname:port/db_number'\nExample without DB and password: 'redis://localhost:6379'\nDefault: none, no Redis connection.");
        options.addOption("mdns", "publishDNSSdServices", false,
                "Publish leshan's services to DNS Service discovery" + X509Chapter);

        options.addOption("ks", "keystore", true,
                "Set the key store file.\nIf set, X.509 mode is enabled, otherwise built-in RPK credentials are used.");
        options.addOption("ksp", "storepass", true, "Set the key store password.");
        options.addOption("kst", "storetype", true,
                String.format("Set the key store type.\nDefault: %s.", DEFAULT_KEYSTORE_TYPE));
        options.addOption("ksa", "alias", true, String.format(
                "Set the key store alias to use for server credentials.\nDefault: %s.\n All other alias referencing a certificate will be trusted.",
                DEFAULT_KEYSTORE_ALIAS));
        options.addOption("ksap", "keypass", true, "Set the key store alias password to use.");

        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.setOptionComparator(null);

        // Parse arguments
        CommandLine cl;
        try {
            cl = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            formatter.printHelp(USAGE, options);
            return;
        }

        // Print help
        if (cl.hasOption("help")) {
            formatter.printHelp(USAGE, options);
            return;
        }

        // Abort if unexpected options
        if (cl.getArgs().length > 0) {
            System.err.println("Unexpected option or arguments : " + cl.getArgList());
            formatter.printHelp(USAGE, options);
            return;
        }

        // get local address
        String localAddress = cl.getOptionValue("lh");
        String localPortOption = cl.getOptionValue("lp");
        int localPort = LwM2m.DEFAULT_COAP_PORT;
        if (localPortOption != null) {
            localPort = Integer.parseInt(localPortOption);
        }

        // get secure local address
        String secureLocalAddress = cl.getOptionValue("slh");
        String secureLocalPortOption = cl.getOptionValue("slp");
        int secureLocalPort = LwM2m.DEFAULT_COAP_SECURE_PORT;
        if (secureLocalPortOption != null) {
            secureLocalPort = Integer.parseInt(secureLocalPortOption);
        }

        // get http address
        String webAddress = cl.getOptionValue("wh");
        String webPortOption = cl.getOptionValue("wp");
        int webPort = 8080;
        if (webPortOption != null) {
            webPort = Integer.parseInt(webPortOption);
        }

        // Get models folder
        String modelsFolderPath = cl.getOptionValue("m");

        // get the Redis hostname:port
        String redisUrl = cl.getOptionValue("r");

        // Get keystore parameters
        String keyStorePath = cl.getOptionValue("ks");
        String keyStoreType = cl.getOptionValue("kst", KeyStore.getDefaultType());
        String keyStorePass = cl.getOptionValue("ksp");
        String keyStoreAlias = cl.getOptionValue("ksa");
        String keyStoreAliasPass = cl.getOptionValue("ksap");

        // Get mDNS publish switch
        Boolean publishDNSSdServices = cl.hasOption("mdns");

        try {
            createAndStartServer(webAddress, webPort, localAddress, localPort, secureLocalAddress, secureLocalPort,
                    modelsFolderPath, redisUrl, keyStorePath, keyStoreType, keyStorePass, keyStoreAlias,
                    keyStoreAliasPass, publishDNSSdServices);
        } catch (BindException e) {
            System.err.println(
                    String.format("Web port %s is already used, you could change it using 'webport' option.", webPort));
            formatter.printHelp(USAGE, options);
        } catch (Exception e) {
            LOG.error("Jetty stopped with unexpected error ...", e);
        }
    }

    public static void createAndStartServer(String webAddress, int webPort, String localAddress, int localPort,
            String secureLocalAddress, int secureLocalPort, String modelsFolderPath, String redisUrl,
            String keyStorePath, String keyStoreType, String keyStorePass, String keyStoreAlias,
            String keyStoreAliasPass, Boolean publishDNSSdServices) throws Exception {
        // Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(localAddress, localPort);
        builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);

        // Create CoAP Config
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanServerBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }
        builder.setCoapConfig(coapConfig);

        // Connect to redis if needed
        Pool<Jedis> jedis = null;
        if (redisUrl != null) {
            // TODO: support sentinel pool and make pool configurable
            jedis = new JedisPool(new URI(redisUrl));
        }

        X509Certificate serverCertificate = null;

        // Set up X.509 mode
        if (keyStorePath != null) {
            try {
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                    keyStore.load(fis, keyStorePass == null ? null : keyStorePass.toCharArray());
                    List<Certificate> trustedCertificates = new ArrayList<>();
                    for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
                        String alias = aliases.nextElement();
                        if (keyStore.isCertificateEntry(alias)) {
                            trustedCertificates.add(keyStore.getCertificate(alias));
                        } else if (keyStore.isKeyEntry(alias) && alias.equals(keyStoreAlias)) {
                            List<X509Certificate> x509CertificateChain = new ArrayList<>();
                            Certificate[] certificateChain = keyStore.getCertificateChain(alias);
                            if (certificateChain == null || certificateChain.length == 0) {
                                LOG.error("Keystore alias must have a non-empty chain of X509Certificates.");
                                System.exit(-1);
                            }

                            for (Certificate certificate : certificateChain) {
                                if (!(certificate instanceof X509Certificate)) {
                                    LOG.error("Non-X.509 certificate in alias chain is not supported: {}", certificate);
                                    System.exit(-1);
                                }
                                x509CertificateChain.add((X509Certificate) certificate);
                            }

                            Key key = keyStore.getKey(alias,
                                    keyStoreAliasPass == null ? new char[0] : keyStoreAliasPass.toCharArray());
                            if (!(key instanceof PrivateKey)) {
                                LOG.error("Keystore alias must have a PrivateKey entry, was {}",
                                        key == null ? null : key.getClass().getName());
                                System.exit(-1);
                            }
                            builder.setPrivateKey((PrivateKey) key);
                            serverCertificate = (X509Certificate) keyStore.getCertificate(alias);
                            builder.setCertificateChain(
                                    x509CertificateChain.toArray(new X509Certificate[x509CertificateChain.size()]));
                        }
                    }
                    builder.setTrustedCertificates(
                            trustedCertificates.toArray(new Certificate[trustedCertificates.size()]));
                }
            } catch (KeyStoreException | IOException e) {
                LOG.error("Unable to initialize X.509.", e);
                System.exit(-1);
            }
        }
        // Otherwise, set up RPK mode
        else {
            try {
                PrivateKey privateKey = SecurityUtil.privateKey.readFromResource("credentials/server_privkey.der");
                serverCertificate = SecurityUtil.certificate.readFromResource("credentials/server_cert.der");
                builder.setPrivateKey(privateKey);
                builder.setCertificateChain(new X509Certificate[] { serverCertificate });

                // Use a certificate verifier which trust all certificates by default.
                Builder dtlsConfigBuilder = new DtlsConnectorConfig.Builder();
                dtlsConfigBuilder.setCertificateVerifier(new CertificateVerifier() {
                    @Override
                    public void verifyCertificate(CertificateMessage message, DTLSSession session)
                            throws HandshakeException {
                        // trust all means never raise HandshakeException
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                });
                builder.setDtlsConfig(dtlsConfigBuilder);

            } catch (Exception e) {
                LOG.error("Unable to load embedded X.509 certificate.", e);
                System.exit(-1);
            }
        }

        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/", modelPaths));
        if (modelsFolderPath != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath)));
        }
        LwM2mModelProvider modelProvider = new StaticModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        // Set securityStore & registrationStore
        EditableSecurityStore securityStore;
        if (jedis == null) {
            // use file persistence
            securityStore = new FileSecurityStore();
        } else {
            // use Redis Store
            securityStore = new RedisSecurityStore(jedis);
            builder.setRegistrationStore(new RedisRegistrationStore(jedis));
        }
        builder.setSecurityStore(securityStore);

        // Create and start LWM2M server
   final LeshanServer lwServer = builder.build();

        // Now prepare Jetty
        InetSocketAddress jettyAddr;
        if (webAddress == null) {
            jettyAddr = new InetSocketAddress(webPort);
        } else {
            jettyAddr = new InetSocketAddress(webAddress, webPort);
        }
        Server server = new Server(jettyAddr);
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setResourceBase(LeshanServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);
        server.setHandler(root);

        // Create Servlet
        EventServlet eventServlet = new EventServlet(lwServer, lwServer.getSecuredAddress().getPort());
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*");

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder = new ServletHolder(new SecurityServlet(securityStore, serverCertificate));
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(new ObjectSpecServlet(lwServer.getModelProvider()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

        // Register a service to DNS-SD
        if (publishDNSSdServices) {

            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            // Publish Leshan HTTP Service
            ServiceInfo httpServiceInfo = ServiceInfo.create("_http._tcp.local.", "leshan", webPort, "");
            jmdns.registerService(httpServiceInfo);

            // Publish Leshan CoAP Service
            ServiceInfo coapServiceInfo = ServiceInfo.create("_coap._udp.local.", "leshan", localPort, "");
            jmdns.registerService(coapServiceInfo);

            // Publish Leshan Secure CoAP Service
            ServiceInfo coapSecureServiceInfo = ServiceInfo.create("_coaps._udp.local.", "leshan", secureLocalPort, "");
            jmdns.registerService(coapSecureServiceInfo);
        }

        // Start Jetty & Leshan
        lwServer.start();
        server.start();
        LOG.info("Web server started at {}.", server.getURI());
       
          //subscribe for registration trackers.
        lwServer.getRegistrationService().addListener(new RegistrationListener() {

            public void registered(Registration registration, Registration previousReg,
                    Collection<Observation> previousObsersations) {
            	try {
            		parkingspotObservationListener parkingspotListener = new parkingspotObservationListener();
            		parkingspotListener.setLeshanServer(lwServer);
            		parkingspotListener.setRegistration(registration);
            		lwServer.getObservationService().addListener(parkingspotListener);

        			ObserveResponse carObserverResponse = lwServer.send(registration, new ObserveRequest(3345,0,5703));
        			if (carObserverResponse.isSuccess()) {
        			System.out.println("carObserverResponse = " + (carObserverResponse.toString()));
                    Observation joystickobservation = carObserverResponse.getObservation();
        			System.out.println("observation itself = " + (joystickobservation.toString()));
        			
        			}else {
        			System.out.println("Failed to read:" + carObserverResponse.getCode() + " " + carObserverResponse.getErrorMessage());
        			}
        			
        			ObserveResponse parkingSpotObserverResponse = lwServer.send(registration, new ObserveRequest(32700,0,32801));
        			if (parkingSpotObserverResponse.isSuccess()) {
        			System.out.println("carObserverResponse = " + (parkingSpotObserverResponse.toString()));
                    Observation joystickobservation = parkingSpotObserverResponse.getObservation();
        			System.out.println("observation itself = " + (parkingSpotObserverResponse.toString()));
        			
        			}else {
        			System.out.println("Failed to read:" + parkingSpotObserverResponse.getCode() + " " + parkingSpotObserverResponse.getErrorMessage());
        			}
        		} catch (InterruptedException e) {
        			e.printStackTrace();
        		}
                
                String sql = "INSERT INTO table1 (parkspot_status, free_until)" +
           			 " VALUES (\"free\", datetime(\"now\") )";
                System.out.println("SQL insert string: " + sql);

                try (Connection conn = DriverManager.getConnection(DatabaseUrl);
                        Statement stmt = conn.createStatement()) {
                    // create a new table
                    stmt.execute(sql);
                    LOG.info("signed up the new parkingspot in the database");
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                    LOG.info("Not able to sign up in the database");
                }
                System.out.println("new device: " + registration.getEndpoint());                      
            }

            public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {
                System.out.println("device is still here: " + updatedReg.getEndpoint());
            }

            public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                    Registration newReg) {
            	//TODO make a database entry here!
                System.out.println("device left: " + registration.getEndpoint());
            }
        });
        LOG.info("Web server started at {}.", server.getURI());
    }
    

    private static class parkingspotObservationListener implements ObservationListener {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicBoolean receivedNotify = new AtomicBoolean();
        private ObserveResponse response;
        private Exception error;
        private LeshanServer leshanServer;
        private Registration parkingspotRegistration;
        private float currentJoyStickValue = 0;
        private String currentOccupationStatus = "";
        
        public void setLeshanServer(LeshanServer aLeshanServer)
        {
        	this.leshanServer = aLeshanServer;
        }
        
        public void setRegistration(Registration aRegistration)
        {
        	this.parkingspotRegistration = aRegistration;
        }

        @Override
        public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
            receivedNotify.set(true);
 //           System.out.format("an observation has been made\n");
            String payload = response.getContent().toString();
//            System.out.format("this is the string I'm going to split up: " + payload +  "\n");
            if(payload.contains("id=5703"))
            {
            	float previousJoyStickValue = currentJoyStickValue;
            	String[] splitupstrings = payload.split("value=");
//                System.out.format("this is the string I'm going to split up next:" + splitupstrings[1] +  "\n");
                splitupstrings = splitupstrings[1].split(",");
                String actualvalue = splitupstrings[0];
//                System.out.format("I hope this is the correct value " + actualvalue +  "\n");
                currentJoyStickValue = Float.parseFloat(actualvalue);
                if(currentJoyStickValue != previousJoyStickValue)
                {
                	if(currentJoyStickValue == 100.0)
                	{
            			sendOccupationStatus(leshanServer, parkingspotRegistration, "occupied");
                	}
                	else if(currentJoyStickValue == -100.0)
                	{
            			sendOccupationStatus(leshanServer, parkingspotRegistration, "free");
                	}
                }
            }
            else if (payload.contains("id=32801"))
            {
//            	System.out.format("this is the string I'm going to split up: " + payload +  "\n");

            	String previousOccupationStatus = currentOccupationStatus;
            	String[] splitupstrings = payload.split("value=");
                splitupstrings = splitupstrings[1].split(",");
                currentOccupationStatus = splitupstrings[0].toLowerCase();
                if(previousOccupationStatus != currentOccupationStatus)
                {
                	switch (currentOccupationStatus.toLowerCase()) {
                	case "free":
            			sendColor(leshanServer, parkingspotRegistration, "green");
            			break;
                	case "reserved":
            			sendColor(leshanServer, parkingspotRegistration, "orange");
//                    	currentJoyStickValue = 1337; //this being the reserved number
            			break;
                	case "occupied":
            			sendColor(leshanServer, parkingspotRegistration, "red");
            			break;
                	}
                }
            }
            else
            {
            	System.out.println("unparsable content:" + (response.getContent().toString()));
            }
            
            this.response = response;
            this.error = null;
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            receivedNotify.set(true);
            this.response = null;
            this.error = error;
            latch.countDown();
        }

        @Override
        public void cancelled(Observation observation) {
            latch.countDown();
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
        }

        public AtomicBoolean receivedNotify() {
            return receivedNotify;
        }

        public ObserveResponse getResponse() {
            return response;
        }

        public Exception getError() {
            return error;
        }

        public void waitForNotification(long timeout) throws InterruptedException {
            latch.await(timeout, TimeUnit.MILLISECONDS);
        }       

    }
}
