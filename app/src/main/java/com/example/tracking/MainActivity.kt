package com.example.tracking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.example.tracking.databinding.ActivityMainBinding
import retrofit.RetrofitManager
import utils.Constant
import java.util.*
import kotlin.math.pow
import kotlin.math.abs

//lat ,Y => 37
//lon ,X => 127

class MainActivity : Activity(),LocationListener,SensorEventListener {
    private lateinit var binding: ActivityMainBinding
    lateinit var text1 : TextView
    lateinit var text2 : TextView
    lateinit var button : Button

    //위치정보를 얻기 위한 변수
    private var locationManager: LocationManager? = null
    var lastKnownLocation: Location? = null
    var lat : Double = 0.0
    var lon : Double = 0.0
    var positionNum = 0 // 초기에 위치를 잡을 때 최소한의 횟수

    //세분화된 좌표를 저장할 배열
    var midpointList = arrayListOf<List<Double>>() //[0]=>lat, [1]=>lon

    //api를 통해 얻은 JSON을 파싱해서 가져온 이중배열 좌표
    var rawRoute = arrayListOf<List<Double>>() //[0]=>lon, [1]=>lat

    //경로 이탈에 사용한 변수
    var i = 0

    //목적지 좌표
    var destinationPoint = arrayListOf<Double>() //[0]=>lat, [1]=>lon

    //경로탐색시 startname과 endname은 중요하지 않기 때문에 그냥 아무거난 만듦.
    var startname="%EC%B6%9C%EB%B0%9C"
    var endname="%EB%B3%B8%EC%82%AC"

    //음성출력관련
    private lateinit var tts : TextToSpeech

    //진동관련
    lateinit var vibrator : Vibrator

    //나침반관련
    private lateinit var mSensorManager : SensorManager
    private lateinit var mAccelerometer : Sensor
    private lateinit var mMagnetometer : Sensor
    private val mLastAccelerometer = FloatArray(3)
    private val mLastMagnetometer = FloatArray(3)
    private var mLastAccelerometerSet = false
    private var mLastMagnetometerSet = false
    private val mR = FloatArray(9)
    private val mOrientation = FloatArray(3)
    private var mCurrentDegree = 0f
    var azimuthinDegress : Float = 0f //기기가 가리키는 나침반 숫자



    //여기서부터 onCreate
    //여기서부터 onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        text1 = findViewById<TextView>(R.id.text)
        text2 = findViewById<TextView>(R.id.text2)
        button = findViewById<Button>(R.id.button)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator


        // TTS를 생성하고 OnInitListener로 초기화 한다.
        tts= TextToSpeech(this){
            if(it==TextToSpeech.SUCCESS){
                val result = tts?.setLanguage(Locale.KOREAN)
                if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                    Log.d("로그","지원하지 않은 언어")
                    return@TextToSpeech
                }
                Log.d("로그","TTS 세팅 성공")
            }else{
                Log.d("로그","TTS 세텅 실패")
            }
        }

        //나침반관련 변수 초기화
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)



        ///////////////////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////
        //위치관련
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        ttsSpeak("현재 위치 조정중입니다.")

        if(lastKnownLocation!=null){
            lon = lastKnownLocation!!.longitude
            lat = lastKnownLocation!!.altitude

            text1.text = java.lang.Double.toString(lon)
            text2.text = java.lang.Double.toString(lat)

            locationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,  //0.5초마다
                0.5f, //0.5미터마다 위치 갱신
                gpsLocationListener
            )
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////

    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //현재위치 추적 및 경로 추적
    val gpsLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lon = location.longitude
            lat = location.latitude

            text1.text = java.lang.Double.toString(lon)
            text2.text = java.lang.Double.toString(lat)

            if(positionNum==3){
                ttsSpeak("위치 조정이 완료되었습니다. 버튼을 눌러 목적지를 말하세요.")
            }
            positionNum++

            //경로이탈인지 아닌지 판단
            if(midpointList.isNotEmpty()){
                if (getDistance(lat, lon, midpointList[i][0],midpointList[i][1])>2){  //p1에서 멀어졌는데
                    if(getDistance(lat,lon,midpointList[i+1][0],midpointList[i+1][1])>2){  //p2에서도 멀어졌다.
                        //경로이탈
                        Log.d("로그경로이탈","${i}"+"번째 point")
                        Log.d("로그경로이탈","현재위치 : "+"${lat}"+", "+"${lon}")
                        Log.d("로그경로이탈","p1 : "+"${midpointList[i][0]}"+", "+"${midpointList[i][1]}")
                        Log.d("로그경로이탈","p2 : "+"${midpointList[i+1][0]}"+", "+"${midpointList[i+1][1]}")
                        ttsSpeak("경로를 이탈했습니다. 경로를 재탐색합니다.")

                        //경로이탈 시 재탐색
                        getRoute(lon,lat,destinationPoint[1],destinationPoint[0],startname,endname)
                        i=0
                    }
                    else{
                        if(i<midpointList.size-1){
                            i++
                        }
                    }
                }
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //api호출 함수
    fun getPOI(location : String){
        destinationPoint.clear()
        RetrofitManager.instance.searchPOI(searchKeyword = location,completion = {
                responseState, parsePOIDataArray ->

            when(responseState){
                Constant.RESPONSE_STATE.OKAY->{  //만약 STATE가 OKEY라면
                    Log.d("로그", " POI api호출 성공")
                    if (parsePOIDataArray != null) {
                        Log.d("로그", "결과 좌표 : "+"${parsePOIDataArray.get(0).frontLat}"+", "+"${parsePOIDataArray.get(0).frontLon}")
                        ttsSpeak("${parsePOIDataArray.get(0).name}"+" 으로 안내합니다.")
                        destinationPoint.add(parsePOIDataArray.get(0).frontLat.toDouble())
                        destinationPoint.add(parsePOIDataArray.get(0).frontLon.toDouble())

                        //rawRoute를 얻음
                        getRoute(lon,lat,destinationPoint[1],destinationPoint[0],startname,endname)
                        //getRoute(127.0690745902964,37.83296140345568,destinationPoint[1],destinationPoint[0],startname,endname)
                    }
                }
                Constant.RESPONSE_STATE.FAIL->{//만약 STATE가 FAIL라면
                    Log.d("로그", " POIapi호출 실패")
                }
                Constant.RESPONSE_STATE.NO_CONTENT->{//만약 NO_CONTENT가 FAIL라면
                    Log.d("로그", " POI 결과가 없습니다.")
                }
            }
        })
    }

    fun getRoute(startx : Double, starty : Double, endx : Double, endy : Double, startname : String, endname : String){
        Log.d("로그", "MainActivity - ROUTE 버튼이 클릭되었다. /")

        // 길찾기 호출
        //manager에서 인터페이스를 가져오고 호출함수 사용하고
        RetrofitManager.instance.searchRoute(startX = startx,
            startY = starty,
            endX = endx,
            endY = endy,
            startname = startname,
            endname = endname,
            completion = {
                    responseState, parseRouteDataArray ->
                when(responseState){
                    Constant.RESPONSE_STATE.OKAY->{  //만약 STATE가 OKEY라면
                        Log.d("로그", " ROUTE api호출 성공")
                        if (parseRouteDataArray != null) {
                            for( p in parseRouteDataArray){
                                rawRoute.add(p.coordinates)
                            }
                            Log.d("로그","rawRoute = "+"${rawRoute}")

                            //중간좌표초기화
                            midpointList.clear()

                            //중간좌표를 얻음.
                            for( i in rawRoute.indices){
                                if(i<rawRoute.size-1){
                                    midPoint(rawRoute[i][1], rawRoute[i][0], rawRoute[i+1][1], rawRoute[i+1][0], midpointList)
                                }
                            }
                            LogLineBreak(midpointList.toString())


                            ttsSpeak("시계를 올려 방향을 찾으세요.")
                            adjustHeading(midpointList[0][0],midpointList[0][1])
                        }


                    }
                    Constant.RESPONSE_STATE.FAIL->{//만약 STATE가 FAIL라면
                        Log.d("로그", " ROUTE api호출 실패")
                    }
                    Constant.RESPONSE_STATE.NO_CONTENT->{//만약 NO_CONTENT가 FAIL라면
                        Log.d("로그", " ROUTE 결과가 없습니다.")
                    }
                }
            })
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //방향조정관련
    fun adjustHeading(lat2: Double,lon2: Double){
        Log.d("로그","adjustHeading호출")
        //여기서 lat2와 lon2는 현재좌표에서 이동할 다음좌표
        while (true){
            if(abs(azimuthinDegress-getAngle(lat,lon,lat2,lon2))<3f){
                vibrator.vibrate(1000)
                break
            }
        }
    }

    fun getAngle(lat1: Double,lon1: Double,lat2: Double,lon2: Double) : Float{
        var y1 = lat1*Math.PI/180
        var y2 = lat2*Math.PI/180
        var x1 = lon1*Math.PI/180
        var x2 = lon2*Math.PI/180

        var x=Math.sin(x2-x1)*Math.cos(y2)
        var y=Math.cos(y1)*Math.sin(y2)-Math.sin(y1)*Math.cos(y2)*Math.cos(x2-x1)
        var rad=Math.atan2(x,y)
        var bearing : Float=((rad*180/Math.PI+360)%360).toFloat()
        return bearing
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //두 좌표를 주면 그 사이의 좌표를 이중배열형태로 저장해줌.
    fun midPoint(lat1: Double, lon1: Double, lat2: Double, lon2: Double, midpointList : ArrayList<List<Double>>) {
        if(getDistance(lat1,lon1,lat2,lon2)>1) {
            var nlat1 = lat1
            var nlon1 = lon1
            var nlat2 = lat2
            val dLon = Math.toRadians(lon2 - lon1)

            //convert to radians
            nlat1 = Math.toRadians(nlat1)
            nlat2 = Math.toRadians(nlat2)
            nlon1 = Math.toRadians(nlon1)
            val Bx = Math.cos(nlat2) * Math.cos(dLon)
            val By = Math.cos(nlat2) * Math.sin(dLon)

            var lat3 = Math.atan2(
                Math.sin(nlat1) + Math.sin(nlat2),
                Math.sqrt((Math.cos(nlat1) + Bx) * (Math.cos(nlat1) + Bx) + By * By)
            )
            var lon3 = nlon1 + Math.atan2(By, Math.cos(nlat1) + Bx)

            lat3 = Math.toDegrees(lat3)
            lon3 = Math.toDegrees(lon3)

            midpointList.add(listOf(lat3, lon3))

            midPoint(lat1, lon1, lat3, lon3, midpointList)
            midPoint(lat3, lon3, lat2, lon2, midpointList)
        }
    }

    fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) + Math.sin(dLon / 2).pow(2.0) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        val c = 2 * Math.asin(Math.sqrt(a))
        return (6372.8 * 1000 * c)
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    override fun onStart() {  //실제 사용자 권한
        super.onStart()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //권한이 없을 경우 최초 권한 요청 또는 사용자에 의한 재요청 확인
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                // 권한 재요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    100
                )
                return
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    100
                )
                return
            }
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //물리버튼을 눌러 STT를 실행
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            Log.d("로그","뒤로가기버튼누름.");
            displaySpeechRecognizer()
            return true;
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun displaySpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results!![0]
                }
            Log.d("로그","STT : "+"${spokenText}");

            if (spokenText != null) {
                getPOI(spokenText)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////




    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //음성출력
    private fun ttsSpeak(strTTS:String){
        tts.speak(strTTS,TextToSpeech.QUEUE_ADD,null,null)
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //긴 로그 출력
    fun LogLineBreak(str: String) {
        if (str.length > 3000) {    // 텍스트가 3000자 이상이 넘어가면 줄
            Log.d("로그long", str.substring(0, 3000))
            LogLineBreak(str.substring(3000))
        } else {
            Log.d("로그long", str)
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //나침반관련
    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_GAME)
        mSensorManager.registerListener(this,mMagnetometer,SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this, mAccelerometer)
        mSensorManager.unregisterListener(this, mMagnetometer)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor == mAccelerometer) {
                System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.size)
                mLastAccelerometerSet = true
            } else if (event.sensor == mMagnetometer) {
                System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.size)
                mLastMagnetometerSet = true
            }
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer)
            azimuthinDegress = ((Math.toDegrees(SensorManager.getOrientation(mR, mOrientation)[0].toDouble()) + 360).toInt() % 360).toFloat()
            mCurrentDegree = -azimuthinDegress
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) { }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////




    override fun onLocationChanged(p0: Location) { }
}