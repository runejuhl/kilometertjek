#!/usr/bin/env bb
(ns petardo.kilometertjek
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]))

(defn crude-html-strip
  "Crudely remove HTML tags from string `s`."
  [s]
  (clojure.string/replace
    s
    #"^(?:<\w+ />)*<[^>]+?>(.+)</[^>]+?>(?:<\w+ />)*$"
    "$1"))

(defn crude-html-strip*
  "Repeatedly apply crude HTML tag strippping until no more tags are present."
  [s]
  (loop [ss  s
         acc nil]
    (if (= acc ss)
      acc
      (recur (crude-html-strip ss) ss))))

(defn curl-get*
  [& args]
  (let [{:keys [status body] :as response} (apply curl/get args)]
    (when (= status 200)
      body)))

(defn get-license-plates
  [car-identifier]
  (some->> (str
             "https://nummerpladeregister.dk/"
             car-identifier)
    (curl-get*)
    (clojure.string/split-lines)
    (map clojure.string/trim)
    (filter #(re-find (re-pattern (str "/" car-identifier)) %))
    (map crude-html-strip*)))

(defn -plate->details
  [plate]
  (println "Fetching plate" plate)
  (some->> plate
    (str "https://www.tjekbil.dk/api/v2/nummerplade/")
    (curl-get*)
    (json/parse-string)))

(def plate->details
  (memoize -plate->details))

(defn map-interval
  "Wait `interval` ms between applying `f` to individual items in `coll`."
  [f coll interval]
  (reduce (fn [acc x]
            (if (seq coll)
              (Thread/sleep interval))
            (cons
              (f x)
              acc))
    coll))

(comment

  (def car-identifier "peugeot/308/2011/16-hdi-112hk-sw")

  (def plates
    (get-license-plates car-identifier))

  (def all-details
    (map-interval plate->details plates 1000))

  ;; (def all-details
  ;;   (map plate->details plates))

  (def details-map
    (into {}
      (map
        (fn [{:strs [StelNr] :as details}] [StelNr details])
        all-details)))

  (map (fn [{:strs [KoeretoejId]}] KoeretoejId) all-details)

  (->> (vals details-map)
    (map #(get % "KoeretoejMotorKilometerstand") )
    (filter identity)
    (sort)))

(comment
  (get details-map "VF34H9HR8AS341658")
  {"KoeretoejId"                              1020301201111205
   "FoersteRegistreringDato"                  "2011-02-10T00:00:00+01:00"
   "AntalDoere"                               nil
   "SynResultatSynsDato"                      "2019-01-30T00:00:00+01:00"
   "TilkoblingsvaegtUdenBremser"              750
   "MotorStoerrelse"                          1.6
   "MiljoeOplysningCO2Udslip"                 nil
   "MotorKilometerstand"                      286
   "PassagerAntal"                            nil
   "Tilkoblingsmulighed"                      true
   "StelNr"                                   "VF34H9HR8AS341658"
   "SporviddenBagest"                         nil
   "LeasingPeriode"                           nil
   "SekundaerStatusDato"                      "2011-02-10T00:00:00+01:00"
   "RegNr"                                    "DF54158"
   "MiljoeOplysningEmissionCO"                nil
   "MiljoeOplysningEmissionHCPlusNOX"         nil
   "MiljoeOplysningRoegtaethed"               nil
   "DrivkraftTypeNavn"                        "Diesel"
   "Status"                                   "Registreret"
   "KoeretoejMotorKilometerstand"             286000
   "MotorMaerkning"                           nil
   "StatusDato"                               "2011-02-10T00:00:00+01:00"
   "FoersteRegistreringDatoFormateret"        "10. februar 2011"
   "KoeretoejAnvendelseNavn"                  "Privat personkørsel"
   "NCAPTest"                                 nil
   "MaksimumHastighed"                        nil
   "MaerkeTypeNavn"                           "PEUGEOT"
   "TraekkendeAksler"                         nil
   "PaahaengVognTotalVaegt"                   nil
   "StelNummerAnbringelse"                    nil
   "SiddepladserMinimum"                      1
   "NormTypeNavn"                             "Ingen Norm"
   "ModelTypeNavn"                            "308"
   "ReparationsIndex"                         83
   "MotorKoerselStoej"                        nil
   "MiljoeOplysningEmissionNOX"               nil
   "FaelgDaek"                                nil
   "TekniskTotalVaegt"                        2150
   "AdressePostNummer"                        nil
   "MotorKmPerLiter"                          19.2
   "MiljoeOplysningRoegtaethedOmdrejningstal" nil
   "MotorSlagVolumen"                         1560.0
   "LeasingGyldigTil"                         nil
   "VariantTypeNavn"                          "1,6 HDI 112HK SW"
   "TotalVaegt"                               2150
   "FarveTypeNavn"                            "Ukendt"
   "KoeretoejUdstyrSamling"                   []
   "StoersteAkselTryk"                        nil
   "KarrosseriTypeNavn"                       nil
   "EgenVaegt"                                nil
   "AdressePostNummerBy"                      nil
   "MotorCylinderAntal"                       nil
   "MotorStandStoejOmdrejningstal"            nil
   "SynResultatSynsResultat"                  "Godkendt"
   "MiljoeOplysningPartikelFilter"            true
   "ModelAar"                                 "-"
   "Koeretoejstand"                           nil
   "VogntogVaegt"                             nil
   "MotorStoersteEffekt"                      82.0
   "TilkoblingsvaegtMedBremser"               1000
   "SporviddenForrest"                        nil
   "KoereklarVaegtMinimum"                    1563
   "AkselAfstand"                             nil
   "AkselAntal"                               2
   "KoeretoejArtNavn"                         "Personbil"
   "MiljoeOplysningPartikler"                 nil
   "GennemsnitsPris"                          7507
   "LeasingGyldigFra"                         nil
   "SynResultatSynsType"                      "PeriodiskSyn"
   "MotorStandStoej"                          nil
   "MotorHestekraefter"                       111
   "MotorElektriskForbrug"                    nil
   "SekundaerStatus"                          "Registreret"}


(plate->details "CS60177")
{"KoeretoejId"                              1020301201115959
 "FoersteRegistreringDato"                  "2011-05-25T00:00:00+02:00"
 "AntalDoere"                               nil
 "SynResultatSynsDato"                      "2019-05-16T00:00:00+02:00"
 "TilkoblingsvaegtUdenBremser"              750
 "MotorStoerrelse"                          1.6
 "MiljoeOplysningCO2Udslip"                 nil
 "MotorKilometerstand"                      178
 "PassagerAntal"                            nil
 "Tilkoblingsmulighed"                      true
 "StelNr"                                   "VF34H9HR8AS270487"
 "SporviddenBagest"                         nil
 "LeasingPeriode"                           nil
 "SekundaerStatusDato"                      "2020-05-26T10:53:59+02:00"
 "RegNr"                                    "CS60177"
 "MiljoeOplysningEmissionCO"                nil
 "MiljoeOplysningEmissionHCPlusNOX"         nil
 "MiljoeOplysningRoegtaethed"               nil
 "DrivkraftTypeNavn"                        "Diesel"
 "Status"                                   "Registreret"
 "KoeretoejMotorKilometerstand"             178000
 "MotorMaerkning"                           nil
 "StatusDato"                               "2020-05-26T10:53:59+02:00"
 "FoersteRegistreringDatoFormateret"        "25. maj 2011"
 "KoeretoejAnvendelseNavn"                  "Privat personkørsel"
 "NCAPTest"                                 nil
 "MaksimumHastighed"                        nil
 "MaerkeTypeNavn"                           "PEUGEOT"
 "TraekkendeAksler"                         nil
 "PaahaengVognTotalVaegt"                   nil
 "StelNummerAnbringelse"                    nil
 "SiddepladserMinimum"                      1
 "NormTypeNavn"                             "Euro V"
 "ModelTypeNavn"                            "308"
 "ReparationsIndex"                         83
 "MotorKoerselStoej"                        nil
 "MiljoeOplysningEmissionNOX"               nil
 "FaelgDaek"                                nil
 "TekniskTotalVaegt"                        2150
 "AdressePostNummer"                        nil
 "MotorKmPerLiter"                          19.2
 "MiljoeOplysningRoegtaethedOmdrejningstal" nil
 "MotorSlagVolumen"                         1560.0
 "LeasingGyldigTil"                         nil
 "VariantTypeNavn"                          "1,6 HDI 112HK SW"
 "TotalVaegt"                               2150
 "FarveTypeNavn"                            "Ukendt"
 "KoeretoejUdstyrSamling"                   []
 "StoersteAkselTryk"                        nil
 "KarrosseriTypeNavn"                       nil
 "EgenVaegt"                                nil
 "AdressePostNummerBy"                      nil
 "MotorCylinderAntal"                       nil
 "MotorStandStoejOmdrejningstal"            nil
 "SynResultatSynsResultat"                  "Godkendt"
 "MiljoeOplysningPartikelFilter"            true
 "ModelAar"                                 "-"
 "Koeretoejstand"                           nil
 "VogntogVaegt"                             nil
 "MotorStoersteEffekt"                      82.0
 "TilkoblingsvaegtMedBremser"               1000
 "SporviddenForrest"                        nil
 "KoereklarVaegtMinimum"                    1563
 "AkselAfstand"                             nil
 "AkselAntal"                               2
 "KoeretoejArtNavn"                         "Personbil"
 "MiljoeOplysningPartikler"                 nil
 "GennemsnitsPris"                          7507
 "LeasingGyldigFra"                         nil
 "SynResultatSynsType"                      "PeriodiskSyn"
 "MotorStandStoej"                          nil
 "MotorHestekraefter"                       111
 "MotorElektriskForbrug"                    nil
 "SekundaerStatus"                          "Registreret"})
