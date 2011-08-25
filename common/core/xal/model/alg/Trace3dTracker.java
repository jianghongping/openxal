/*
 * EnvelopeTracker.java
 *
 *  Created on September 9, 2002, 11:14 AM
 *  Modified:
 *      12/02   - CKA
 *      9/03    - CKA: added space charge interval prediction
 *                   : added rotation into ellipsoid coordinates
 *
 */

package xal.model.alg;

import xal.model.IElement;
import xal.model.IProbe;
import xal.model.ModelException;
import xal.model.probe.EnvelopeProbe;
import xal.tools.beam.CovarianceMatrix;
import xal.tools.beam.PhaseIndexHom;
import xal.tools.beam.PhaseMap;
import xal.tools.beam.PhaseMatrix;
import xal.tools.beam.em.BeamEllipsoid;
import xal.tools.math.r3.R3;


/**
 * <p>
 * Tracking algorithm for <code>EnvelopeProbe</code>'s.  The <code>EnvelopeProbe</code>'s
 * state, which is a <code>CovarianceMatrix</code> object, is advanced using the linear
 * dynamics portion of any beamline element (<code>IElement</code> exposing object) transfer
 * map.  The linear portion is represented as a matrix, thus, the state advance is accomplished
 * with a transpose conjugation with this matrix.
 * </p>
 * <p>
 * The effects of space charge are also included in the dynamics calculations.  Space charge
 * effects are also represented with a matrix transpose conjugation, however, the matrix is
 * computation using the values of the probe's correlation matrix.  The result is a nonlinear
 * effect.  The space charge forces are computed using a linear fit to the fields generated by
 * an ellipsoidal charge distribution with the same statistics described in the probe's 
 * correlation matrix.  The linear fit is weighted by the beam distribution itself, so it is 
 * more accurate in regions of higher charged density.  For a complete description see the reference
 * below.
 * </p>
 * <p>
 * Completed: Replace <code>EllipsoidalCharge</code> with <code>BeamEllipsoid</code>.
 * </p>
 * 
 * @see xal.tools.beam.em.BeamEllipsoid
 * @see <a href="http://lib-www.lanl.gov/cgi-bin/getfile?00796950.pdf">Theory and Technique
 *      of Beam Envelope Simulation</a>
 *
 * @author Christopher K. Allen
 * @author Craig McChesney
 */
public class Trace3dTracker extends Tracker {

    /*
     *  Global Attributes
     */
    
    /** string type identifier for algorithm */
    public static final String      s_strTypeId = "Trace3dTracker";
    
    /** current algorithm version */
    public static final int         s_intVersion = 1;
    
    /** probe type recognized by this algorithm */
    public static final Class<EnvelopeProbe>       s_clsProbeType = EnvelopeProbe.class;
    
    /** maximum distance to advance probe before applying space charge kick */
    private static final double     s_dblMaxStep = .01;  
    
    
    
    /*
     *  Local Attributes
     */
     
     
     
    
    /*
     * Initialization
     */

    /** 
     *  Creates a new instance of EnvelopeTracker 
     */
    public Trace3dTracker() { 
        super(s_strTypeId, s_intVersion, s_clsProbeType);
    };
    

    
    
    /**
     *  Accessing
     */
    
    /**
     * Returns the maximum element subsection length (in meters) that the probe 
     * may be advanced before applying a space charge kick.
     * 
     * @return  maximum step size for numerical integration 
     */
    private double getMaxStepSize() {
    	return s_dblMaxStep;
    }
    



    /*
     *  Tracker Abstract Methods
     */
    
    /**
     * Propagates the probe through the element.
     *
     *  @param  probe   probe to propagate
     *  @param  elem    element acting on probe
     *
     *  @exception  ModelException  invalid probe type or error in advancing probe
     */
    @Override
    public void doPropagation(IProbe probe, IElement elem) throws ModelException {
        
        int     nSteps = (int) Math.max(Math.ceil(elem.getLength() / getMaxStepSize()), 1);
        double  dblSize = elem.getLength() / nSteps;
        for (int i=0 ; i<nSteps ; i++) {
            this.advanceState(probe, elem, dblSize);
            this.advanceProbe(probe, elem, dblSize);
            probe.update();
        }
    }



    
    
    /*
     *  Internal Support Functions
     */
    
    
    /** 
     *  Advances the probe state through a subsection of the element with the
     *  specified length.  Applies a space charge kick at the end of the element
     *  subsection for any probe having nonzero beam current.
     *
     *  @param  ifcElem     interface to the beam element
     *  @param  ifcProbe    interface to the probe
     *  @param  dblLen      length of element subsection to advance through
     *
     *  @exception ModelException     bad element transfer matrix/corrupt probe state
     */
    protected void advanceState(IProbe ifcProbe, IElement ifcElem, double dblLen) 
            throws ModelException {
        
        // Identify probe
        EnvelopeProbe   probe = (EnvelopeProbe)ifcProbe;
       
        // Get initial conditions of probe
        double              gamma = probe.getGamma();
        double              K    = probe.beamPerveance();
        CovarianceMatrix   chi0 = probe.getCorrelation();
        
        // Get properties of the element
        double      L    = dblLen;
        PhaseMap    mapE = ifcElem.transferMap(ifcProbe, L);
        PhaseMatrix PhiE = mapE.getFirstOrder();
        
        // Get the space charge kick
        PhaseMatrix  PhiSC = this.spaceChargeMatrix(K, L, gamma, chi0);
        
        // Advance the probe through the element
        PhaseMatrix Phi  = PhiE.times(PhiSC);
        PhaseMatrix chi1 = chi0.conjugateTrans(Phi);
        
        
        // Save the new state variables in the probe
        probe.setCorrelation(new CovarianceMatrix(chi1));
    };

    /** 
     * <p>
     * Calculates the transfer matrix for a space charge kick.
     * </p>
     * <p>
     * <h4>NOTE:</h4>
     *  <em>&middot; This currently works only for upright beam ellipses
     *  in configuration space!</em>
     *  <br/>
     *  &middot; This method was converted from the use of <code>EllipsoidalCharge</code>
     *  to class <code>BeamEllipsoid</code> to make the space charge calculations. 
     *  Hopefully there are no side effects.
     *  </p>
     * 
     *  @param  K       beam generalized perveance (3D bunched beam)
     *  @param  dL      propagation distance
     *  @param  gamma   relativistic factor
     *  @param  matChi  envelope correlation matrix in homogeneous coordinates
     * 
     *  @return         matrix representing linear space charge effects
     * 
     *  @author Christopher K. Allen
     */
    private PhaseMatrix spaceChargeMatrix(double K, double dL, double gamma, CovarianceMatrix matChi)    {
        
        // Check for zero-space charge case
        if (K==0.0 || dL==0.0) 
            return PhaseMatrix.identity();

        // Build the beam charge density object and compute (de)focusing strengths from space charge
//        EllipsoidalCharge   rho = new EllipsoidalCharge(K, matChi);
        BeamEllipsoid       rho = new BeamEllipsoid(gamma, matChi);
        
//        R3  vecFocus = rho.compDefocusConstantsAlaTrace3D();
        double[] arrFocus = BeamEllipsoid.compDefocusConstantsAlaTrace3D(gamma, rho.get2ndMoments());
        R3       vecFocus = new R3(arrFocus); 
        
        double kX = (K*dL)/vecFocus.getx();
        double kY = (K*dL)/vecFocus.gety();
        double kZ = (K*dL)/vecFocus.getz();

        // Get the beam displacement from the origin and the rotation matrix
//        R3          vecDis = rho.getDisplacement();
        PhaseMatrix matDis = rho.getTranslation();
        R3          vecDis = new R3(
                matDis.getElem(PhaseIndexHom.X, PhaseIndexHom.HOM),
                matDis.getElem(PhaseIndexHom.Y, PhaseIndexHom.HOM),
                matDis.getElem(PhaseIndexHom.Z, PhaseIndexHom.HOM)
                );

//        PhaseMatrix matRot = rho.compPhaseRotation();
        PhaseMatrix matRot = rho.getRotation();
        
        // Convert to class BeamEllipsoid
//        BeamEllipsoid   rho = new BeamEllipsoid(K, matChi);
//
//        double[]    arrDeFocus = rho.getDefocusingConstants();
//        double kX = (K*dL)/arrDeFocus[0];
//        double kY = (K*dL)/arrDeFocus[1];
//        double kZ = (K*dL)/arrDeFocus[2];
//
//        // Get the beam displacement from the origin and the rotation matrix
//        PhaseMatrix    matT   = rho.getTranslation();
//        R3             vecDis = new R3(matT.getElem(0, 6), matT.getElem(2,6), matT.getElem(4,6));
//        PhaseMatrix    matRot = rho.getRotation();
        
        double  xm = vecDis.getx();
        double  ym = vecDis.gety();
        double  zm = vecDis.getz();
                 

        // Assemble the space charge matrix in the ellipsoid semi-axes coordinates
        PhaseMatrix matSC = PhaseMatrix.identity();

        matSC.setElem(1,0,  kX);
        matSC.setElem(1,6, -kX*xm);
        matSC.setElem(3,2,  kY);
        matSC.setElem(3,6, -kY*ym);
        matSC.setElem(5,4,  kZ);
        matSC.setElem(5,6, -kZ*zm);
        
        matSC = matSC.conjugateTrans(matRot);   // now rotate to beam cartesian coordinates
        
        return matSC;
    };
}



/*
 *  Storage
 */
 
 
//    /** 
//     * Calculates the transfer matrix for a space charge kick.
//     * 
//     * NOTE:
//     *  <b>This currently works only for upright beam ellipses
//     *  in configuration space!</b>
//     * 
//     *  @param  K       beam generalized perveance (3D bunched beam)
//     *  @param  dL      propagation distance
//     *  @param  matChi  envelope correlation matrix in homogeneous coordinates
//     * 
//     *  @return         matrix representing linear space charge effects
//     * 
//     *  @author Christopher K. Allen
//     */
//    private PhaseMatrix spaceChargeMatrix(double K, double dL, double gamma, CovarianceMatrix matChi)    {
//        
//        // Check for zero-space charge case
//        if (K==0.0 || dL==0.0) 
//            return PhaseMatrix.identity();
//
//        // Build the beam charge density object  
////        EllipsoidalCharge   rho = new EllipsoidalCharge(matChi);
//        
//        double  a = Math.sqrt(matChi.getCovXX());
//        double  b = Math.sqrt(matChi.getCovYY());
//        double  c = Math.sqrt(matChi.getCovZZ());
//        R3      vecOff = new R3(matChi.getMeanX(), matChi.getMeanY(), matChi.getMeanZ());
//        EllipsoidalCharge   rho = new EllipsoidalCharge(a, b, gamma*c);
//        rho.setDisplacement(vecOff);
//                        
//        
//        // Compute (de)focusing strengths from space charge
//        R3  vecFocus = rho.compDefocusConstants();
////        R3  vecFocus = rho.compDefocusConstantsAlaTrace3D();
//        
//        double kX = (K*dL)/vecFocus.getx();
//        double kY = (K*dL)/vecFocus.gety();
//        double kZ = (K*dL)/vecFocus.getz();
//
//        // Get the beam displacement from the origin
//        R3  vecDispl = rho.getDisplacement();
//        
//        double  xm = vecDispl.getx();
//        double  ym = vecDispl.gety();
//        double  zm = vecDispl.getz();
//                 
//
//        // Assemble the space charge matrix
//        PhaseMatrix matSC = PhaseMatrix.identity();
//
//        matSC.setElem(1,0,  kX);
//        matSC.setElem(1,6, -kX*xm);
//        matSC.setElem(3,2,  kY);
//        matSC.setElem(3,6, -kY*ym);
//        matSC.setElem(5,4,  kZ);
//        matSC.setElem(5,6, -kZ*zm);
//        
//        return matSC;
//    };
//    
//}


 