CSGDatabase.clear()
LengthParameter printerOffset 			= new LengthParameter("printerOffset",0.5,[1.2,0])
LengthParameter boltlen 			= new LengthParameter("Bolt Length",0.5,[1.2,0])
def motorOptions = []
def shaftOptions = []
for(String vitaminsType: Vitamins.listVitaminTypes()){
	HashMap<String, Object> meta = Vitamins.getMeta(vitaminsType);
	if(meta !=null && meta.containsKey("actuator"))
		motorOptions.add(vitaminsType);
	if(meta !=null && meta.containsKey("shaft"))
		shaftOptions.add(vitaminsType);
}
StringParameter motors = new StringParameter("Motor Type","roundMotor",motorOptions)
StringParameter shafts = new StringParameter("Shaft Type","dShaft",shaftOptions)
StringParameter motorSize = new StringParameter("Motor Size","WPI-gb37y3530-50en",Vitamins.listVitaminSizes(motors.getStrValue()))
StringParameter shaftSize = new StringParameter("Shaft Size","WPI-gb37y3530-50en",Vitamins.listVitaminSizes(shafts.getStrValue()))

def motorBlank= Vitamins.get(motors.getStrValue(),motorSize.getStrValue())
def shaftBlank= Vitamins.get(shafts.getStrValue(),shaftSize.getStrValue())
double motorAngleOffset = 65
double knuckelThicknessAdd = 2
double pitch = 6
double pinRadius = ((3/16)*25.4+printerOffset.getMM())/2
double pinLength = (16)+ (printerOffset.getMM()*2)
double actualBoltLength = 35
double boltPattern = 10
String size ="M5"
HashMap<String, Object>  boltData = Vitamins.getConfiguration( "capScrew",size)
HashMap<String, Object>  nutData = Vitamins.getConfiguration( "lockNut",size)
CSG nut =Vitamins.get("lockNut",size)

CSG nutKeepaway =nut.hull().makeKeepaway(printerOffset.getMM())
double nutHeight = nut.getMaxZ()
double washerRadius =  boltData.outerDiameter/2+printerOffset.getMM()*2

if(args == null){
	args=[
		Vitamins.get("ballBearing","695zz"),
		new Cylinder(pinRadius,pinRadius,pinLength,(int)30).toCSG(), // steel reenforcmentPin
		new Cylinder(pinRadius,pinRadius,pinLength,(int)30).toCSG().movez(-pinLength/2) // steel reenforcmentPin
		
	]
}
double washerThickness = motorBlank.getMaxZ()-args[0].getTotalZ()
def washer = new Cylinder(washerRadius,washerThickness).toCSG()
			.difference(new Cylinder(boltData.outerDiameter/2+printerOffset.getMM()/2,washerThickness).toCSG())
def washerKeepaway = 	new Cylinder(washerRadius+printerOffset.getMM(),washerThickness).toCSG()	

println "Bolt"+boltData
println "nut"+nutData
println "Pin len ="+pinLength

double linkMaterialThickness = pinLength/2

double washerOd = 17
double washerId = 12.75
double bearingThickness = args[0].getMaxZ()
double gearThickness =(pinLength -(bearingThickness*3 -washerThickness*2))/2

double encoderToEncoderDistance =args[0].getMaxX()*3+
					gearThickness*2 -
					washerThickness*4 +
					args[0].getMaxZ()*2+
					bearingThickness*2+
					nutHeight*2

int aTeeth =    Math.PI*(encoderToEncoderDistance+washerThickness*2)/pitch
int bTeeth =    Math.PI*(args[0].getMaxX()+washerThickness*2+gearThickness+nutHeight)*2/pitch 

// call a script from another library
List<Object> bevelGears = (List<Object>)ScriptingEngine
					 .gitScriptRun(
            "https://github.com/madhephaestus/GearGenerator.git", // git location of the library
            "bevelGear.groovy" , // file to load
            // Parameters passed to the funcetion
            [	  aTeeth,// Number of teeth gear a
	            bTeeth,// Number of teeth gear b
	            gearThickness,// thickness of gear A
	            pitch,// gear pitch in arch length mm
	           90
            ]
            )
List<Object> spurGears = (List<Object>)ScriptingEngine
					 .gitScriptRun(
            "https://github.com/madhephaestus/GearGenerator.git", // git location of the library
            "bevelGear.groovy" , // file to load
            // Parameters passed to the funcetion
            [	  bTeeth,// Number of teeth gear a
	            12,// Number of teeth gear b
	            gearThickness,// thickness of gear A
	            pitch,// gear pitch in arch length mm
	           0,
	           20
            ]
            )
//Print parameters returned by the script
println "Bevel gear radius A " + bevelGears.get(2)
println "Bevel gear radius B " + bevelGears.get(3)
println "Spur gear radius A " + spurGears.get(2)
println "Spur gear radius B " + spurGears.get(3)
double ratio1 = (spurGears[7])
double ratio2 = (bevelGears[7])
double ratio = ratio1*ratio2
println "Total Wrist Ratio is 1=" +ratio1+" 2="+ratio2+" total = "+ratio
encoderToEncoderDistance = bevelGears.get(2)*2
double outerBearingDistance = (encoderToEncoderDistance/2+gearThickness+washerThickness+args[0].getTotalZ())*2
double gearBThickness =bevelGears.get(6)
double distanceToShaft =bevelGears.get(3)
double distancetoGearFace = bevelGears.get(2)
double distanceToMotor = bevelGears.get(3)+spurGears[2]+spurGears[3]
double shaftToMotor = spurGears[2]+spurGears[3]
double knuckelY = args[0].getTotalY()+knuckelThicknessAdd*2
double knuckelZ = distanceToShaft+(knuckelY/2)-gearThickness+knuckelThicknessAdd+nut.getMaxX()
double knuckelX = encoderToEncoderDistance-(gearBThickness*2)-1
def centeredSpur = spurGears[1].movex((spurGears[2]+spurGears[3]))
				.toZMax()
//return centeredSpur

def spurs =[spurGears[1],spurGears[0]].collect{
		it.roty(-90)
		.movez(distanceToShaft)
		.movex(distancetoGearFace)
}

CSG drivenA = spurs[1]
def spursB = spurs.collect{
	it.rotz(180)
}

CSG drivenB = spursB[1]


CSG outputGear = bevelGears.get(0)
CSG adrive = bevelGears.get(1).rotz(180).union(drivenA)
CSG bdrive = bevelGears.get(1).union(drivenB)
// return the CSG parts

double bearingLocation =  adrive.getMinX()-washerThickness
CSG innerBearing = args[0].hull()
			.toZMin()
			.roty(-90)
			.toXMax()
			.movex( bearingLocation)
			.movez(bevelGears.get(3))

CSG bearing =args[0].hull()
			.toZMin()
			.roty(-90)
			.movex(  encoderToEncoderDistance/2+gearThickness+washerThickness)
			.movez(bevelGears.get(3))
def bearingHeight = bearingThickness+washerThickness+gearThickness
def boltlenvalue= washerThickness*2+bearingThickness*2+gearBThickness+gearThickness+nutHeight
println "Bolt length ="+boltlenvalue
boltlen.setMM(actualBoltLength+gearThickness*2)
CSG bolt = Vitamins.get("capScrew",size)
			.roty(180)
			.toZMax()
			.movez(nut.getMaxZ() +(actualBoltLength-boltlenvalue))
bearing=CSG.unionAll([bearing,
		bearing.rotz(180),
		args[0].hull().toZMax().movez(bearingHeight),
		args[0].hull().toZMax().movez(knuckelZ+gearThickness+1),
		innerBearing,
		innerBearing.rotz(180)
		])

CSG motor = 	args[2]
			.roty(-90)
			.movez(	distanceToMotor)

def nutLocations =[
new Transform()
	.translate(0,0,knuckelZ+gearThickness+1)// X , y, z	
 ,
 new Transform()
		.translate( -bearingLocation+ bearingThickness+washerThickness/4,0,distanceToShaft)// X , y, z
		.rot( 0, -90, 0) // x,y,z
  ,
  new Transform()
		.translate(bearingLocation-bearingThickness-washerThickness/4,0,distanceToShaft)// X , y, z		
		.rot( 0, 90, 0) // x,y,z
		
]
def outerPlateLocationL = new Transform().roty(-90).movez(distanceToShaft).movex(-(encoderToEncoderDistance/2+gearThickness+washerThickness))
def outerPlateLocationR= new Transform().roty(90).movez(distanceToShaft).movex(encoderToEncoderDistance/2+gearThickness+washerThickness)

def washerLocations =[
new Transform().movez(gearThickness),
new Transform().roty(-90).movez(distanceToShaft).movex(bearingLocation),
outerPlateLocationR,
new Transform().roty(90).movez(distanceToShaft).movex(-bearingLocation),
outerPlateLocationL
]

def allWashers = washerLocations.collect{
	washer.transformed(it)
}
double upperNutsZ = distanceToShaft+args[0].getMaxX()+3.5
double lowerNutsZ = distanceToShaft-args[0].getMaxX()-3.5
def mountBoltHeight = gearThickness-1
def mountLocations =[
new Transform().translate(boltPattern,boltPattern,mountBoltHeight),
new Transform().translate(-boltPattern,-boltPattern,mountBoltHeight),
new Transform().translate(-boltPattern,boltPattern,mountBoltHeight),
new Transform().translate(boltPattern,-boltPattern,mountBoltHeight),
new Transform()
	.rotx(90)
	.movez(upperNutsZ)
	.movey(args[0].getMaxX()+knuckelThicknessAdd+2)
	.movex(washerThickness+args[0].getTotalZ()+2),
new Transform()
	.rotx(90)
	.movez(upperNutsZ)
	.movey(args[0].getMaxX()+knuckelThicknessAdd+2)
	.movex(-(washerThickness+args[0].getTotalZ()+2)),
new Transform()
	.rotx(90)
	.movez(lowerNutsZ)
	.movey(args[0].getMaxX()+knuckelThicknessAdd+2)
	.movex(washerThickness+args[0].getTotalZ()+2),
new Transform()
	.rotx(90)
	.movez(lowerNutsZ)
	.movey(args[0].getMaxX()+knuckelThicknessAdd+2)
	.movex(-(washerThickness+args[0].getTotalZ()+2))
	
]
def boltKeepaway = bolt.toolOffset(printerOffset.getMM())
def NutKW =CSG.unionAll(Extrude.revolve(nut.hull().makeKeepaway(printerOffset.getMM()),
		(double)0, // rotation center radius, if 0 it is a circle, larger is a donut. Note it can be negative too
		(double)65,// degrees through wich it should sweep
		(int)5))
		
def nuts = nutLocations.collect{
	NutKW.movez(-0.75).transformed(it)
}
def bolts = nutLocations.collect{
	boltKeepaway.transformed(it)
}
def mountNuts = mountLocations.collect{
	nutKeepaway.movez(-0.5).transformed(it)
}
nuts.addAll(mountLocations.collect{
	nut.transformed(it)
})
def mountBolts = mountLocations.collect{
	boltKeepaway.transformed(it)
}


def sweep = new Cylinder(encoderToEncoderDistance/2, // Radius at the bottom
                      		encoderToEncoderDistance/2, // Radius at the top
                      		nut.getTotalZ()+2, // Height
                      		(int)30 //resolution
                      		).toCSG()//convert to CSG to display      
			.difference(new Cylinder(args[0].getMaxY()+3, // Radius at the bottom
                      		args[0].getMaxY()+3, // Radius at the top
                      		nut.getTotalZ()+2, // Height
                      		(int)30 //resolution
                      		).toCSG())
                .movez(gearThickness)
               
outputGear=outputGear
			.union()
			.difference(mountNuts)
			.difference(mountBolts)


def washerKW = allWashers.collect{it.hull().toolOffset(1)}
def knuckel = new Cube(knuckelX,knuckelY,knuckelZ).toCSG()
				.toZMin()
				.movez(gearThickness+0.5)
				.difference(sweep)
				.difference(nuts)
				.difference(bolts)
				.difference(washerKW)
				.difference(bearing)
				.difference(mountBolts)
				.difference(mountNuts)
				
def bbox = knuckel.getBoundingBox()
			.toYMin()
def knuckelLeft = knuckel.intersect(bbox)
def knuckelRigth = knuckel.difference(bbox)


double distToGearEdge = encoderToEncoderDistance/2+gearThickness

def MotorLoacations = [
new Transform()
	.roty(-90).movez(shaftToMotor).rotx(motorAngleOffset).movez(distanceToShaft).movex(distToGearEdge),
new Transform()
	.roty(90).movez(shaftToMotor).rotx(motorAngleOffset).movez(distanceToShaft).movex(-distToGearEdge)
]
double boltMountHeight =distanceToShaft*2-mountBoltHeight+ knuckelThicknessAdd
double upperPlateBoltPattern  = boltPattern+7
double motorBrackerTHick = washerThickness+args[0].getTotalZ()-printerOffset.getMM()
def mountLocationsOuterUpper =[
new Transform().roty(-90).movex(outerBearingDistance/2).movey(upperPlateBoltPattern),
new Transform().roty(-90).movex(outerBearingDistance/2).movey(-upperPlateBoltPattern),
new Transform().roty(90).movex(-outerBearingDistance/2).movey(upperPlateBoltPattern),
new Transform().roty(90).movex(-outerBearingDistance/2).movey(-upperPlateBoltPattern)
].collect{
	it.movez(boltMountHeight+nut.getMaxZ()*2)
}
def driveGearsFinal = MotorLoacations.collect{
	centeredSpur.difference (shaftBlank
	.toZMax()).transformed(it)
}
def allMotors = MotorLoacations.collect{
	motorBlank
	.toZMax()
	.rotx(180)
	.transformed(it)
}
def allShafts = MotorLoacations.collect{
	shaftBlank
	.toZMax()
	.transformed(it)
}
double plateTHick = nut.getMaxZ()*2
def upperMountLocations = mountLocations.subList(0, 4).collect{
	it.movez(boltMountHeight)
}
def upperNuts = upperMountLocations.collect{
	nut.roty(180).transformed(it)
}
def uppermountNuts = upperMountLocations.collect{
	nutKeepaway.movez(-1).roty(180).transformed(it)
}
def upperBOlt = Vitamins.get("capScrew",size)
			.movez(nut.getMaxZ()*2)
			.toolOffset(printerOffset.getMM())
boltlen.setMM(65)
def sideUpperBolt = Vitamins.get("capScrew",size)
			.toolOffset(printerOffset.getMM())
def uppermountBolts = upperMountLocations.collect{
	upperBOlt.transformed(it)
}

def upperSidemountBolts = mountLocationsOuterUpper.collect{
	sideUpperBolt.transformed(it)
}
double mountBrackerY = upperSidemountBolts.get(0).getMaxY()*2
def bracket = new Cube( outerBearingDistance-(motorBrackerTHick)*2 +printerOffset.getMM()*2,
					mountBrackerY,
					plateTHick).toCSG()
			.toZMin()
			.movez(boltMountHeight+nut.getMaxZ())
			.difference(upperSidemountBolts)
			.difference(uppermountBolts)
			.difference(uppermountNuts)
def boltLug = new Cube( motorBrackerTHick-printerOffset.getMM(),
					mountBrackerY,
					plateTHick).toCSG()
			.toZMin()
			.movez(boltMountHeight+nut.getMaxZ())
def motorHold = new Cube(motorBlank.getTotalX()+5,motorBlank.getTotalY()+5,motorBrackerTHick-printerOffset.getMM()).toCSG()
				.toZMin()
				.toYMin()
				.movey(-motorBlank.getMaxY()-2.5)
def bearingLug = new Cube( motorBrackerTHick-printerOffset.getMM(),
					args[0].getTotalY()+2.5,
					args[0].getTotalY()+2.5).toCSG()
			.movez(distanceToShaft)	
boltLug=boltLug.union(	bearingLug)					
def motorHoldL = motorHold.transformed(	MotorLoacations.get(0)).movex(printerOffset.getMM())			
def motorHoldR = motorHold.transformed(	MotorLoacations.get(1)).movex(-printerOffset.getMM())
		
def boltLugL = boltLug.toXMin().movex(bracket.getMaxX()).union(motorHoldL)	.hull()
def boltLugR = boltLug.toXMax().movex(bracket.getMinX()).union(motorHoldR).hull()

def motorBracketSets = [boltLugL,boltLugR].collect{
	it.difference(upperSidemountBolts)
	.difference(allMotors)
	.difference(bolts)
	.difference(washerKW)
	.difference(bearing)
}
def  releifHole= new Cylinder(bolt.getMaxX()+1,plateTHick).toCSG()
			.movez(bracket.getMinZ())
bracket=bracket.difference(releifHole)
double upperDistLinkLen =  (boltLugL.getMaxZ()-distanceToShaft)

println "Bottom to shaft "+ distanceToShaft
println "Shaft to top  "+ upperDistLinkLen
return [outputGear,adrive,bdrive,
//bearing,nuts,bolts,
allWashers,knuckelLeft,knuckelRigth,driveGearsFinal,
//upperNuts,
bracket,
motorBracketSets,
]
