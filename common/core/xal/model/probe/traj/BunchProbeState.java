/*
 * BunchProbeState.java
 *
 * Created on April, 2003, 5:15 PM
 * 
 * Modifications:
 *  11/2006     - CKA removed references to Twiss parameters 
 *                and correlation matrix
 *              - CKA changed primary state variables to 
 *                beam current and bunch frequency
 */

package xal.model.probe.traj;

import xal.tools.data.DataAdaptor;
import xal.tools.math.r3.R3;

import xal.model.probe.BunchProbe;
import xal.model.xml.ParsingException;


/**
 * Encapsulates a BunchProbe's state at a point in time.  Contains
 * addition state variables for probes with beam-like behavior.
 * 
 * @author Craig McChesney
 * @author Christopher K. Allen
 * @version $id:
 * 
 */
public abstract class BunchProbeState extends ProbeState /* implements IPhaseState */ {


    /*
     * Global Constants
     */

    // ************ I/O Support
    /** element tag for beam state data */    
    private static final String ELEM_BEAM = "beam";
    
    /** attribute tag for total beam current */
    private static final String ATTR_BEAMCURRENT = "I";
    
    /** attribute tag for total beam charge */
    private static final String ATTR_BUNCHFREQ = "f";
    
    /** attribute tag for betatron phase advance */    
    private static final String ATTR_BETAPHASE = "phase";
    


    /*
     * Local Attributes
     */
     
    /** bunch frequency in Hz */
    private double  dlbBunFreq = 0.0;
    
    /** Beam current */
    private double  dblBmCurr = 0.0;
    
    /** betatron phase of bunch in three phase planes */
    protected R3    vecPhsBeta;


//  /** Beam charge */
//  private double m_dblBmQ = 0.0;
//

    /*
     * Required Implementation
     */    
    
//    /** 
//     *  Abstract - Returns the correlation matrix (sigma matrix) in homogeneous
//     *  phase space coordinates.
//     *
//     *  @return         <zz^T> =| <x*x>  <x*xp>  <x*y>  <x*yp>  <x*z>  <x*zp>  <x>  |
//     *                          | <xp*x> <xp*xp> <xp*y> <xp*yp> <xp*z> <xp*zp> <xp> |
//     *                            ...
//     *
//     *  @see    gov.sns.tools.beam.PhaseMatrix
//     */
//    public abstract CovarianceMatrix phaseCorrelation();



    /*
     * Initialization
     */
    
    /**
     * Default constructor.  Creates an empty <code>BunchProbeState</code>.
     *
     */
    public BunchProbeState() {
        this.vecPhsBeta = new R3();
    }
    
    /**
     * Initializing constructor.  Creates a new <code>BunchProbe</code> object initialized 
     * to the argument's state.
     * 
     * @param probe     probe object with which to initialize this state
     */
    public BunchProbeState(BunchProbe probe) {
        super(probe);
        this.setBunchFrequency(probe.getBunchFrequency());
        this.setBeamCurrent(probe.getBeamCurrent());
        this.setBetatronPhase(probe.getBetatronPhase());
    }

    
    
    /**
     * Set the bunch arrival time frequency.
     * 
     * @param f     new bunch frequency in <b>Hz</b>
     */
    public void setBunchFrequency(double f) {
        this.dlbBunFreq = f;
    }
 
    /**
     *  Set the total beam current 
     * 
     * @param   I   new beam current in <bold>Amperes</bold>
     */
    public void setBeamCurrent(double I) {
        dblBmCurr = I;
    }
    
    /**
     * Set the betatron phase of the bunch centroid for each phase plane.
     * 
     * @param   vecPhase vector (psix,psiy,psiz) of betratron phases in <b>radians</b>
     */
    public void setBetatronPhase(R3 vecPhase) {
        this.vecPhsBeta = vecPhase;
    }

//    /**
//     *  Set the total beam charge 
//     * 
//     *  @param  Q   beam charge in <bold>Coulombs</bold>
//     */
//    public void setBeamCharge(double Q) {
//        m_dblBmQ = Q;
//    }

    
    
    /*
     * Attribute Query
     */
    
    /**
     * Returns the bunch frequency, that is the frequency of 
     * the bunches need to create the beam current.
     * 
     * The bunch frequency f is computed from the beam current 
     * I and bunch charge Q as 
     *  
     *      f = I/Q
     *      
     * @return  bunch frequency in Hertz
     */
    public double getBunchFrequency()  {
        return this.dlbBunFreq;
    };
    
    /** 
     * Returns the total beam current 
     * 
     * @return  beam current in <b>amps</b>
     */
    public double getBeamCurrent() {
        return dblBmCurr;
    }
    
    /**
     * Returns the betatron phase with space charge for all three phase
     * planes.
     * 
     * @return  vector (psix,psiy,psiz) of phases in <b>radians</b>
     */
    public R3 getBetatronPhase() {
        return this.vecPhsBeta;
    }
    
 
    /*
     * Computed Properties
     */
    
    /** 
     * Computes and returns the charge in each beam bunch
     * 
     * @return  beam charge in <b>coulombs</b>
     */
    public double bunchCharge() {
        if (this.getBunchFrequency() > 0.0) {
            return this.getBeamCurrent()/this.getBunchFrequency();
            
        } else {
            return 0.0;
            
        }
    }

//
//    /*
//     * CovarianceMatrix Properties
//     */
//
//    
//    /**
//     *  Returns the covariance matrix of this state in homogeneous
//     *  phase space coordinates.
//     * 
//     * @return      <zz^T> - <z><z>^T
//     */
//    public  CovarianceMatrix phaseCovariance()   {
//        return phaseCorrelation().getCovariance();
//    }
//    
//    /**
//     *  Returns the rms emittances for this state.
//     * 
//     * @return array (ex,ey,ez) of rms emittances
//     */
//    public double[] rmsEmittances() {
//		return phaseCorrelation().rmsEmittances();
//    }
//    
//    /**
//     * Return the twiss parameters for this state calculated from the 
//     * correlation matrix
//     * 
//     * @deprecated This method does not provide correct Twiss info with any dipole bend.  Should use getTwiss() from EnvelopeProbe class.
//     */
//    public Twiss[] twissParameters() {
//    	return phaseCorrelation().twissParameters();
//    }
//	
//	
//    /**
//	 * get the array of twiss objects for this state for all three planes
//     * @deprecated This method does not provide correct Twiss info with any dipole bend presented.  Should use getTwiss() from EnvelopeProbe.
//	 * @return array(twiss-H, twiss-V, twiss-L
//	 */
//    public Twiss[] getTwiss() {
//		return twissParameters();
//	}
//	
//    
//    /** 
//     *  Abstract - Return the phase space coordinates of the centroid in homogeneous coordinates 
//     *
//     *  @return         <z> = (<x>, <xp>, <y>, <yp>, <z>, <zp>, 1)^T
//     */
//    public PhaseVector phaseMean()  {
//        return phaseCorrelation().getMean();
//    }
//	
//	
//    /** 
//	 *  Returns homogeneous phase space coordinates of the particle.  The units
//	 *  are meters and radians.
//	 *
//	 *  @return     vector (x,x',y,y',z,z',1) of phase space coordinates
//	 */
//    public PhaseVector phaseCoordinates() {
//		return phaseMean();
//	}
//	
//	
//	/**
//	 * Get the fixed orbit about which betatron oscillations occur.
//	 * @return the fixed orbit vector (x,x',y,y',z,z',1)
//	 */
//	public PhaseVector getFixedOrbit() {
//		return phaseMean();
//	}
//	

    /*
     * Debugging
     */
     
     
    /**
     * Write out state information to a string.
     * 
     * @return     text version of internal state data
     */
    @Override
    public String toString() {
        return super.toString() + 
                " curr: " + getBeamCurrent() + 
                " freq: " + getBunchFrequency() +
                " phase: " + getBetatronPhase();
    }
	


    /*
     * Support Methods
     */	
	
    /**
     * Save the state values particular to <code>BunchProbeState</code> objects
     * to the data sink.
     * 
     *  @param  daSink   data sink represented by <code>DataAdaptor</code> interface
     */
    @Override
    protected void addPropertiesTo(DataAdaptor daSink) {
        super.addPropertiesTo(daSink);
        DataAdaptor datBunch = daSink.createChild(ELEM_BEAM);
        datBunch.setValue(ATTR_BUNCHFREQ,   getBunchFrequency());
        datBunch.setValue(ATTR_BEAMCURRENT, getBeamCurrent());
        datBunch.setValue(ATTR_BETAPHASE,   getBetatronPhase().toString());
        
    }
    
    /**
     * Recover the state values particular to <code>BunchProbeState</code> objects 
     * from the data source.
     *
     *  @param  daSource   data source represented by a <code>IIDataAdaptor</code> interface
     * 
     *  @exception ParsingException     state information in data source is malformatted
     */
    @Override
    protected void readPropertiesFrom(DataAdaptor daSource) 
            throws ParsingException {
        super.readPropertiesFrom(daSource);
        
        DataAdaptor daBunch = daSource.childAdaptor(ELEM_BEAM);
        if (daBunch == null)
            throw new ParsingException("BunchProbeState#readPropertiesFrom(): no child element = " + ELEM_BEAM);

        if (daBunch.hasAttribute(ATTR_BUNCHFREQ))
            setBunchFrequency(daBunch.doubleValue(ATTR_BUNCHFREQ));
        if (daBunch.hasAttribute(ATTR_BEAMCURRENT))            
            setBeamCurrent(daBunch.doubleValue(ATTR_BEAMCURRENT));
        if (daBunch.hasAttribute(ATTR_BETAPHASE)) {
            R3  vecPhase = new R3( daBunch.stringValue(ATTR_BETAPHASE) );
            this.setBetatronPhase( vecPhase );
        }
    }

}
