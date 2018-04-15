LengthParameter printerOffset 			= new LengthParameter("printerOffset",0.5,[1.2,0])

double pinRadius = ((3/16)*25.4+printerOffset.getMM())/2

double pinLength = (2.5*25.4)+ (printerOffset.getMM()*2)
double linkMaterialThickness = pinLength/2
double washerThickness = 1.5
double washerOd = 17
double washerId = 12.75
double gearThickness =8


if(args == null){
	args=[
		Vitamins.get("ballBearing","R8-60355K505"),
		new Cylinder(pinRadius,pinRadius,pinLength,(int)30).toCSG().movez(-pinLength/2), // steel reenforcmentPin
		new Cylinder(pinRadius,pinRadius,pinLength,(int)30).toCSG().movez(-pinLength/2) // steel reenforcmentPin
		
	]
}
double encoderToEncoderDistance = pinLength-args[0].getMaxZ()*2
double pitch = 5
int aTeeth =    Math.PI*(encoderToEncoderDistance -washerThickness*2 )/pitch
int bTeeth =    Math.PI*(args[0].getMaxX()+washerThickness+gearThickness)*2/pitch

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
            [	  aTeeth,// Number of teeth gear a
	            bTeeth,// Number of teeth gear b
	            gearThickness,// thickness of gear A
	            pitch,// gear pitch in arch length mm
	           0,
	           20
            ]
            )
//Print parameters returned by the script
println "Bevel gear radius A " + bevelGears.get(2)
println "Bevel gear radius B " + bevelGears.get(3)
println "Bevel angle " + bevelGears.get(4)
println "Bevel tooth face length " + bevelGears.get(5)
double distanceToShaft =bevelGears.get(3)
double distancetoGearFace = bevelGears.get(2)
double distanceToMotor = bevelGears.get(3)+spurGears[2]+spurGears[3]
def spurs =[spurGears[1],spurGears[0]].collect{
		it.roty(-90)
		.movez(distanceToShaft)
		.movex(distancetoGearFace)
}
CSG driveA = spurs[0]
CSG drivenA = spurs[1]
def spursB = spurs.collect{
	it.rotz(180)
}
CSG driveB = spursB[0]
CSG drivenB = spursB[1]

CSG outputGear = bevelGears.get(0)
CSG adrive = bevelGears.get(1).rotz(180).union(drivenA)
CSG bdrive = bevelGears.get(1).union(drivenB)
// return the CSG parts
gears= [outputGear,adrive,bdrive]

CSG bearing =args[0]
			.toZMin()
			.roty(-90)
			.movex(  encoderToEncoderDistance/2+gearThickness)
			.movez(bevelGears.get(3))
bearing=bearing.union(bearing.rotz(180))
		.union(args[0].movez(gearThickness+washerThickness))
CSG pin  = args[1]
			.roty(-90)
			.movez(bevelGears.get(3))
CSG motor = 	args[2]
			.roty(-90)
			.movez(	distanceToMotor)

return [gears,pin,bearing,driveA,driveB,motor]
